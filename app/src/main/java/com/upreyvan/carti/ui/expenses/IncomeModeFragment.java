package com.upreyvan.carti.ui.expenses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentIncomeModeBinding;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.data.repository.IncomeRepository;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class IncomeModeFragment extends BaseFragment<FragmentIncomeModeBinding> {

    private IncomeAdapter incomeAdapter;
    private IncomeRepository incomeRepository;
    private RealtimeSubscription realtimeSubscription;

    @Override
    protected FragmentIncomeModeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentIncomeModeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        incomeRepository = new IncomeRepository(requireContext());
        setupDynamicPadding();
        setupToolbar();
        setupRecyclerView();
        observeIncomes();
        updateUI();
        setupListeners();
        initRealtime();
    }

    private void initRealtime() {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        String familyId = pref.getFamilyId();
        String userId = pref.getUserId();
        if (familyId.isEmpty() || userId.isEmpty()) return;

        Realtime realtime = new Realtime(AppwriteManager.getInstance(requireContext()).getClient());
        
        String incomesChannel = "databases." + Constants.Appwrite.DATABASE_ID + 
                                ".collections." + Constants.Appwrite.COL_INCOMES + 
                                ".documents";
        String userChannel = "databases." + Constants.Appwrite.DATABASE_ID + 
                             ".collections." + Constants.Appwrite.COL_USERS + 
                             ".documents." + userId;
        
        realtimeSubscription = realtime.subscribe(new String[]{incomesChannel, userChannel}, event -> {
            boolean isIncomeEvent = false;
            for (String channel : event.getChannels()) {
                if (channel.contains(Constants.Appwrite.COL_INCOMES)) {
                    isIncomeEvent = true;
                    break;
                }
            }

            if (isIncomeEvent) {
                requireActivity().runOnUiThread(() -> incomeRepository.refreshIncomes());
            } else {
                requireActivity().runOnUiThread(this::updateUI);
            }
            return null;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (realtimeSubscription != null) {
            realtimeSubscription.close();
        }
    }

    private void setupRecyclerView() {
        incomeAdapter = new IncomeAdapter();
        incomeAdapter.setOnIncomeLongClickListener(income -> {
            IncomeEditBottomSheet bottomSheet = IncomeEditBottomSheet.newInstance(income);
            bottomSheet.setListener(this::updateUI);
            bottomSheet.show(getChildFragmentManager(), "IncomeEditBottomSheet");
        });
        getBinding().rvIncomeHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        getBinding().rvIncomeHistory.setAdapter(incomeAdapter);
    }

    private void observeIncomes() {
        incomeRepository.getUserIncomes().observe(getViewLifecycleOwner(), incomes -> {
            boolean hasIncomes = incomes != null && !incomes.isEmpty();
            getBinding().tvIncomeHistoryLabel.setVisibility(hasIncomes ? View.VISIBLE : View.GONE);
            getBinding().rvIncomeHistory.setVisibility(hasIncomes ? View.VISIBLE : View.GONE);

            incomeAdapter.setIncomes(incomes);
        });

        incomeRepository.getTotalIncome().observe(getViewLifecycleOwner(), totalIncome -> {
            updateTotalBudget(totalIncome != null ? totalIncome : 0.0);
        });

        incomeRepository.refreshIncomes();
    }

    private void updateTotalBudget(double totalBudget) {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        int daysLeft = manager.getDaysUntilNextPayday();
        double dailyBudget = manager.getDailyBudget(totalBudget);

        getBinding().tvSalaryAmount.setText(String.format(Locale.getDefault(), "₱%,.2f", totalBudget));
        getBinding().tvDailyBudget.setText(String.format(Locale.getDefault(), "₱%,.2f / day", dailyBudget));
    }
    

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.income_mode_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.label_add);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> showAddIncomeBottomSheet());
    }

    private void updateUI() {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        
        int daysLeft = manager.getDaysUntilNextPayday();
        getBinding().tvDaysLeft.setText(String.valueOf(daysLeft));
        
        Calendar nextPayday = manager.getNextPayday();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        getBinding().tvTargetDate.setText(sdf.format(nextPayday.getTime()));
        
        incomeRepository.refreshIncomes();
    }

    private void setupListeners() {
        getBinding().cardNextPayday.setOnClickListener(v -> showDatePicker());
        getBinding().cardNextPayday.setOnLongClickListener(v -> {
            showPaydaySettings();
            return true;
        });
    }



    private void showPaydaySettings() {
        PaydayEditBottomSheet bottomSheet = PaydayEditBottomSheet.newInstance();
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "PaydayEditBottomSheet");
    }

    private void showDatePicker() {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        Calendar nextPayday = manager.getNextPayday();

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Update the day of month for the payday schedule
                    if (manager.isMonthly()) {
                        manager.setFirstPayday(dayOfMonth);
                    } else {
                        // Semi-monthly: update the day that was most recently "next"
                        int currentNextDay = nextPayday.get(Calendar.DAY_OF_MONTH);
                        int first = manager.getFirstPayday();
                        int second = manager.getSecondPayday();
                        
                        if (Math.abs(currentNextDay - first) <= Math.abs(currentNextDay - second)) {
                            manager.setFirstPayday(dayOfMonth);
                        } else {
                            manager.setSecondPayday(dayOfMonth);
                        }
                    }
                    updateUI();
                },
                nextPayday.get(Calendar.YEAR),
                nextPayday.get(Calendar.MONTH),
                nextPayday.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showAddIncomeBottomSheet() {
        IncomeEditBottomSheet bottomSheet = IncomeEditBottomSheet.newInstance(IncomeEditBottomSheet.Mode.ADD_INCOME);
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "IncomeAddBottomSheet");
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
