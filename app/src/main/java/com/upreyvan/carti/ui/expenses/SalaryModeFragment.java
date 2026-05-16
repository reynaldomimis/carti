package com.upreyvan.carti.ui.expenses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.databinding.FragmentSalaryModeBinding;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SalaryModeFragment extends BaseFragment<FragmentSalaryModeBinding> {

    @Override
    protected FragmentSalaryModeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentSalaryModeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        fetchTotalIncome();
        fetchCycleExpenses();
        updateUI();
        setupListeners();
    }

    private void fetchCycleExpenses() {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        String startDate = sdf.format(manager.getLastPayday().getTime());
        String endDate = sdf.format(manager.getNextPayday().getTime());

        new com.upreyvan.carti.data.remote.ApiHelper(requireContext()).getTransactions(startDate, endDate, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<io.appwrite.models.DocumentList<java.util.Map<String, Object>>>() {
            @Override
            public void onSuccess(io.appwrite.models.DocumentList<java.util.Map<String, Object>> result) {
                if (!isAdded()) return;
                double totalExpense = 0;
                for (io.appwrite.models.Document<java.util.Map<String, Object>> doc : result.getDocuments()) {
                    java.util.Map<String, Object> data = doc.getData();
                    if ("EXPENSE".equals(data.get("type"))) {
                        Object amountObj = data.get("amount");
                        if (amountObj instanceof Number) {
                            totalExpense += ((Number) amountObj).doubleValue();
                        }
                    }
                }
                final double finalTotal = totalExpense;
                requireActivity().runOnUiThread(() -> {
                    getBinding().tvTotalExpenses.setText(String.format(Locale.getDefault(), "₱%,.2f", finalTotal));
                    updateRemainingBalance(finalTotal);
                });
            }

            @Override
            public void onError(Throwable error) {
            }
        });
    }

    private void updateRemainingBalance(double totalExpense) {
        float salary = SalaryManager.getInstance(requireContext()).getSalaryAmount();
        double remaining = salary - totalExpense;
        getBinding().tvRemainingBalance.setText(String.format(Locale.getDefault(), "₱%,.2f", remaining));
        
        if (remaining < 0) {
            getBinding().tvRemainingBalance.setTextColor(getResources().getColor(R.color.status_red));
        } else {
            getBinding().tvRemainingBalance.setTextColor(getResources().getColor(R.color.carti_primary_green));
        }
    }

    private void fetchTotalIncome() {
        new com.upreyvan.carti.data.remote.ApiHelper(requireContext()).getFamilySummary(new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<io.appwrite.models.Document<java.util.Map<String, Object>>>() {
            @Override
            public void onSuccess(io.appwrite.models.Document<java.util.Map<String, Object>> result) {
                if (!isAdded()) return;
                java.util.Map<String, Object> data = result.getData();
                if (data.containsKey("totalIncome")) {
                    Object income = data.get("totalIncome");
                    float amount = 0f;
                    if (income instanceof Number) {
                        amount = ((Number) income).floatValue();
                    }
                    SalaryManager.getInstance(requireContext()).setSalaryAmount(amount);
                    requireActivity().runOnUiThread(() -> updateUI());
                }
            }

            @Override
            public void onError(Throwable error) {
                // Fail silently or log
            }
        });
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.salary_mode_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void updateUI() {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        
        // Next Salary Card
        int daysLeft = manager.getDaysUntilNextPayday();
        getBinding().tvDaysLeft.setText(String.valueOf(daysLeft));
        
        Calendar nextPayday = manager.getNextPayday();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        getBinding().tvTargetDate.setText(sdf.format(nextPayday.getTime()));

        // Details
        float salaryAmount = manager.getSalaryAmount();
        getBinding().tvSalaryAmount.setText(String.format(Locale.getDefault(), "₱%,.2f", salaryAmount));
        
        if (manager.isMonthly()) {
            int day = manager.getFirstPayday();
            getBinding().tvPaydaySchedule.setText(String.format(Locale.getDefault(), "Every %d%s of the month", 
                day, getDayNumberSuffix(day)));
        } else {
            getBinding().tvPaydaySchedule.setText(getString(R.string.payday_format, manager.getFirstPayday(), manager.getSecondPayday()));
        }

        double dailyBudget = manager.getDailyBudget();
        getBinding().tvDailyBudget.setText(String.format(Locale.getDefault(), "₱%,.2f / day", dailyBudget));

        // Progress Calculation (Spent vs Salary)
        double totalExpense = 0;
        try {
            String spentText = getBinding().tvTotalExpenses.getText().toString();
            totalExpense = Double.parseDouble(spentText.replaceAll("[^0-9.]", ""));
        } catch (Exception ignored) {}

        int spentProgress = (salaryAmount > 0) ? (int) ((totalExpense / (float) salaryAmount) * 100) : 0;
        getBinding().progressSalary.setProgress(Math.min(spentProgress, 100));
        
        // Update labels (Days passed)
        int totalDays = manager.getTotalDaysInCycle();
        int daysPassed = totalDays - daysLeft;
        if (daysPassed < 0) daysPassed = 0;

        getBinding().tvProgressRange.setText(String.format(Locale.getDefault(), "%s - %s", 
            sdf.format(manager.getLastPayday().getTime()), 
            sdf.format(manager.getNextPayday().getTime())));
    }

    private String getDayNumberSuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private void setupListeners() {
        getBinding().btnEditSalary.setOnClickListener(v -> showSalaryEditDialog());
        getBinding().btnEditPayday.setOnClickListener(v -> showPaydayEditDialog());
    }

    private void showSalaryEditDialog() {
        SalaryEditBottomSheet bottomSheet = SalaryEditBottomSheet.newInstance();
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "SalaryEditBottomSheet");
    }

    private void showPaydayEditDialog() {
        PaydayEditBottomSheet bottomSheet = PaydayEditBottomSheet.newInstance();
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "PaydayEditBottomSheet");
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                null,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_height)
        );
    }
}