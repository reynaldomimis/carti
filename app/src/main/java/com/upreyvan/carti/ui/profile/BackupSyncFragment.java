package com.upreyvan.carti.ui.profile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.local.DebtManager;
import com.upreyvan.carti.data.local.ExpenseManager;
import com.upreyvan.carti.data.local.GoalManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentBackupSyncBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.Debt;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BackupSyncFragment extends BaseFragment<FragmentBackupSyncBinding> {

    private ApiHelper apiHelper;
    private PreferenceManager pref;

    @Override
    protected FragmentBackupSyncBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentBackupSyncBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        pref = new PreferenceManager(requireContext());
        setupToolbar();
        setupContent();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.offline_mode_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupContent() {
        getBinding().actionAdd.ivActionIcon.setImageResource(android.R.drawable.ic_input_add);
        getBinding().actionAdd.tvActionName.setText(R.string.action_add_expenses);
        getBinding().actionAdd.getRoot().setOnClickListener(v -> {
            if (getActivity() instanceof com.upreyvan.carti.MainActivity) {
                ((com.upreyvan.carti.MainActivity) getActivity()).navigateTo(7);
            }
        });

        getBinding().actionView.ivActionIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        getBinding().actionView.tvActionName.setText(R.string.action_view_transactions);
        getBinding().actionView.getRoot().setOnClickListener(v -> {
            if (getActivity() instanceof com.upreyvan.carti.MainActivity) {
                ((com.upreyvan.carti.MainActivity) getActivity()).navigateTo(2);
            }
        });

        getBinding().actionSummary.ivActionIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        getBinding().actionSummary.tvActionName.setText(R.string.action_view_summary);
        getBinding().actionSummary.getRoot().setOnClickListener(v -> {
            if (getActivity() instanceof com.upreyvan.carti.MainActivity) {
                ((com.upreyvan.carti.MainActivity) getActivity()).navigateTo(1);
            }
        });

        getBinding().btnSync.setOnClickListener(v -> performManualSync());
    }

    private void performManualSync() {
        getBinding().btnSync.setEnabled(false);
        getBinding().btnSync.setText("Syncing...");

        String lastSync = pref.getLastSyncTime();
        // Use a wide range for full sync if lastSync is empty, otherwise gateway handles delta
        String startDate = "2000-01-01";
        String endDate = "2099-12-31";

        apiHelper.sync(lastSync, startDate, endDate, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (!isAdded()) return;

                processSyncResult(result);

                requireActivity().runOnUiThread(() -> {
                    getBinding().btnSync.setEnabled(true);
                    getBinding().btnSync.setText(R.string.btn_sync_now);
                    
                    // Update last sync time to now
                    String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());
                    pref.setLastSyncTime(now);
                    
                    Toast.makeText(requireContext(), "Sync successful! Data updated.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    getBinding().btnSync.setEnabled(true);
                    getBinding().btnSync.setText(R.string.btn_sync_now);
                    Toast.makeText(requireContext(), "Sync failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void processSyncResult(Map<String, Object> result) {
        // 1. Family / Salary
        Map<String, Object> family = (Map<String, Object>) result.get("family");
        if (family != null) {
            Object b = family.get("balance");
            if (b instanceof Number) {
                SalaryManager.getInstance(requireContext()).setSalaryAmount(((Number) b).floatValue());
            }
        }

        // 2. Categories (If provided in sync, otherwise skip)
        // Note: CategoryManager currently uses defaults + local prefs. 

        // 3. Transactions
        List<Map<String, Object>> transactionsData = (List<Map<String, Object>>) result.get("transactions");
        if (transactionsData != null && !transactionsData.isEmpty()) {
            List<Transaction> transactions = new ArrayList<>();
            CategoryManager categoryManager = CategoryManager.getInstance(requireContext());
            List<Category> categories = categoryManager.getCategories();
            for (Map<String, Object> data : transactionsData) {
                transactions.add(mapToTransaction(data, categories));
            }
            ExpenseManager.getInstance().setTransactions(transactions);
        }

        // 4. Debts
        List<Map<String, Object>> debtsData = (List<Map<String, Object>>) result.get("debts");
        if (debtsData != null && !debtsData.isEmpty()) {
            List<Debt> debts = new ArrayList<>();
            for (Map<String, Object> data : debtsData) {
                debts.add(mapToDebt(data));
            }
            DebtManager.getInstance().setDebts(debts);
        }

        // 5. Goals
        List<Map<String, Object>> goalsData = (List<Map<String, Object>>) result.get("goals");
        if (goalsData != null && !goalsData.isEmpty()) {
            List<Goal> goals = new ArrayList<>();
            for (Map<String, Object> data : goalsData) {
                goals.add(mapToGoal(data));
            }
            GoalManager.getInstance().setGoals(goals);
        }
    }

    private Transaction mapToTransaction(Map<String, Object> data, List<Category> categories) {
        String categoryName = String.valueOf(data.get("category"));
        double amount = 0;
        Object amt = data.get("amount");
        if (amt instanceof Number) amount = ((Number) amt).doubleValue();
        
        String type = String.valueOf(data.get("type"));
        String rawDate = String.valueOf(data.get("$createdAt"));
        String time = Utils.formatIsoDateToTime(rawDate);

        Category cat = null;
        for (Category c : categories) {
            if (c.getName().equalsIgnoreCase(categoryName)) {
                cat = c;
                break;
            }
        }

        int iconRes = (cat != null) ? cat.getIconRes() : R.drawable.ic_chart;
        int iconColor = (cat != null) ? ContextCompat.getColor(requireContext(), cat.getIconColor()) : 0xFF888888;
        int bgColor = (cat != null) ? ContextCompat.getColor(requireContext(), cat.getBackgroundColor()) : 0x1A888888;

        String id = String.valueOf(data.get("$id"));
        String sign = "EXPENSE".equalsIgnoreCase(type) ? "-" : "+";

        return new Transaction(
                id,
                categoryName,
                time,
                sign + "₱" + String.format(Locale.getDefault(), "%,.0f", amount),
                iconRes,
                bgColor,
                iconColor
        );
    }

    private Debt mapToDebt(Map<String, Object> data) {
        String id = String.valueOf(data.get("$id"));
        String name = String.valueOf(data.get("personName"));
        double amount = 0;
        Object amt = data.get("amount");
        if (amt instanceof Number) amount = ((Number) amt).doubleValue();
        boolean isPaid = Boolean.TRUE.equals(data.get("isPaid"));
        return new Debt(id, name, "", "Upcoming", amount, isPaid, android.R.drawable.ic_menu_myplaces, "");
    }

    private Goal mapToGoal(Map<String, Object> data) {
        String id = String.valueOf(data.get("$id"));
        String name = String.valueOf(data.get("name"));
        double target = 0;
        Object t = data.get("targetAmount");
        if (t instanceof Number) target = ((Number) t).doubleValue();
        double current = 0;
        Object c = data.get("currentAmount");
        if (c instanceof Number) current = ((Number) c).doubleValue();
        return new Goal(id, name, current, target, "Target Date", android.R.drawable.ic_menu_compass,
                ContextCompat.getColor(requireContext(), R.color.carti_primary_green));
    }
}