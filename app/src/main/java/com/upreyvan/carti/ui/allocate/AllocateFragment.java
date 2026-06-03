package com.upreyvan.carti.ui.allocate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.databinding.FragmentAllocateBinding;
import com.upreyvan.carti.model.BudgetAllocation;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.Transaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllocateFragment extends BaseFragment<FragmentAllocateBinding> {
    private AllocateAdapter adapter;
    private boolean isTrackMode = false;

    public static AllocateFragment newInstance(boolean track) {
        AllocateFragment f = new AllocateFragment();
        Bundle a = new Bundle(); a.putBoolean("isTrack", track);
        f.setArguments(a); return f;
    }

    @Override
    protected FragmentAllocateBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAllocateBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) isTrackMode = getArguments().getBoolean("isTrack", false);
        setupToolbar();
        setupList();
        loadData();
    }

    private void setupToolbar() {
        getBinding().tvTitle.setText(isTrackMode ? "Expense Tracking" : "Budget Allocation");
        getBinding().btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        getBinding().btnAddAllocation.setVisibility(isTrackMode ? View.GONE : View.VISIBLE);
    }

    private void setupList() {
        adapter = new AllocateAdapter();
        adapter.setTrackMode(isTrackMode);
        getBinding().rvAllocations.setAdapter(adapter);
    }

    private void loadData() {
        new Thread(() -> {
            if (!isAdded() || getContext() == null) return;
            List<BudgetCategoryItem> plan = BudgetManager.getInstance(requireContext()).getBudgetPlan();
            String familyId = PreferenceManager.getInstance(requireContext()).getFamilyId();
            List<Transaction> txs = AppDatabase.getInstance(requireContext()).transactionDao().getAllTransactionsList(familyId);

            Map<String, BudgetAllocation> mainGroups = new HashMap<>();
            Map<String, List<BudgetCategoryItem>> childPlanMap = new HashMap<>();
            Map<String, List<Transaction>> expenseMap = new HashMap<>();

            for (BudgetCategoryItem item : plan) {
                if (item.getParentCategory() == null || item.getParentCategory().isEmpty()) {
                    mainGroups.put(item.getCategoryName(), new BudgetAllocation(
                        item.getCategoryName(), item.getCategoryName(), item.getAmount(), 0, 
                        item.getIconRes(), item.getIconColor()));
                } else {
                    childPlanMap.computeIfAbsent(item.getParentCategory(), k -> new ArrayList<>()).add(item);
                }
            }

            for (Transaction tx : txs) {
                if (!"EXPENSE".equalsIgnoreCase(tx.getType())) continue;
                expenseMap.computeIfAbsent(tx.getCategory(), k -> new ArrayList<>()).add(tx);
            }

            for (Map.Entry<String, BudgetAllocation> entry : mainGroups.entrySet()) {
                String catName = entry.getKey();
                BudgetAllocation parent = entry.getValue();

                List<BudgetCategoryItem> subPlans = childPlanMap.get(catName);
                double totalSpent = 0;

                if (subPlans != null) {
                    for (BudgetCategoryItem sp : subPlans) {
                        double currentSubSpent = 0;
                        List<Transaction> subTxs = expenseMap.get(sp.getCategoryName());
                        if (subTxs != null) for (Transaction t : subTxs) currentSubSpent += t.getAmount();
                        
                        parent.addSubAllocation(new BudgetAllocation(sp.getCategoryName(), sp.getCategoryName(), sp.getAmount(), currentSubSpent, sp.getIconRes(), sp.getIconColor()));
                        totalSpent += currentSubSpent;
                    }
                }

                List<Transaction> directExps = expenseMap.get(catName);
                if (directExps != null) {
                    parent.setExpenses(directExps);
                    if (subPlans == null) {
                        for (Transaction t : directExps) totalSpent += t.getAmount();
                    }
                }
                parent.setCurrentSpent(totalSpent);
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(new ArrayList<>(mainGroups.values()));
                    getBinding().layoutEmpty.setVisibility(mainGroups.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        }).start();
    }
}
