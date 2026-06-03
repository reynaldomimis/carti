package com.upreyvan.carti.ui.allocate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.FragmentPlanBinding;
import com.upreyvan.carti.databinding.ItemBudgetCardBinding;
import com.upreyvan.carti.databinding.ItemCategoryRowBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.util.Utils;
import java.util.Calendar;
import java.util.Locale;

public class PlanFragment extends BaseFragment<FragmentPlanBinding> {
    private PlanViewModel viewModel;
    private GenericAdapter<BudgetCategoryItem, ItemBudgetCardBinding> budgetAdapter;
    private GenericAdapter<BudgetCategoryItem, ItemCategoryRowBinding> categoryAdapter;

    public static PlanFragment newInstance(boolean track) {
        return new PlanFragment();
    }

    @Override
    protected FragmentPlanBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentPlanBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PlanViewModel.class);
        
        setupUI();
        setupAdapters();
        observeViewModel();
        
        setupDynamicPadding();
        viewModel.loadData();
    }

    private void setupDynamicPadding() {
        setupDynamicPadding(getBinding().appBar, getBinding().rootScroll, 0.85f);
    }

    private void setupUI() {
        Calendar cal = Calendar.getInstance();
        String monthYear = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " + cal.get(Calendar.YEAR);
        getBinding().tvCurrentMonth.setText(monthYear);

        getBinding().tabBudget.setOnClickListener(v -> selectTab(v.getId()));
        getBinding().tabBills.setOnClickListener(v -> selectTab(v.getId()));
        getBinding().tabGoals.setOnClickListener(v -> selectTab(v.getId()));

        getBinding().btnAddBudget.setOnClickListener(v -> showAddBudgetBottomSheet());
        getBinding().btnAddCategory.setOnClickListener(v -> showAddCategoryBottomSheet());
    }

    private void selectTab(int id) {
        getBinding().tabBudget.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(id == R.id.tabBudget ? R.color.carti_primary_green : android.R.color.transparent, null)));
        getBinding().tabBudget.setTextColor(getResources().getColor(id == R.id.tabBudget ? R.color.white : R.color.text_secondary, null));
        getBinding().tabBudget.setIconTintResource(id == R.id.tabBudget ? R.color.white : R.color.text_secondary);

        getBinding().tabBills.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(id == R.id.tabBills ? R.color.carti_primary_green : android.R.color.transparent, null)));
        getBinding().tabBills.setTextColor(getResources().getColor(id == R.id.tabBills ? R.color.white : R.color.text_secondary, null));
        getBinding().tabBills.setIconTintResource(id == R.id.tabBills ? R.color.white : R.color.text_secondary);

        getBinding().tabGoals.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(id == R.id.tabGoals ? R.color.carti_primary_green : android.R.color.transparent, null)));
        getBinding().tabGoals.setTextColor(getResources().getColor(id == R.id.tabGoals ? R.color.white : R.color.text_secondary, null));
        getBinding().tabGoals.setIconTintResource(id == R.id.tabGoals ? R.color.white : R.color.text_secondary);
    }

    private void setupAdapters() {
        budgetAdapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemBudgetCardBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.tvStatus.setText(R.string.status_good);
                    binding.tvProgressText.setText(String.format("%s of %s", Utils.formatCurrency(item.getCurrentSpent()), Utils.formatCurrency(item.getAmount())));
                    binding.tvRemainingText.setText(String.format("%s left", Utils.formatCurrency(Math.max(0, item.getAmount() - item.getCurrentSpent()))));
                });

        categoryAdapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemCategoryRowBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.ivCategoryIcon.setImageResource(item.getIconRes() != 0 ? item.getIconRes() : R.drawable.ic_chart);
                    
                    int bgColor = item.getBgColor() != 0 ? item.getBgColor() : R.color.mint_green_alpha;
                    int iconColor = item.getIconColor() != 0 ? item.getIconColor() : R.color.carti_primary_green;
                    
                    binding.cardIcon.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(getResources().getColor(bgColor, null)));
                    binding.ivCategoryIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(iconColor, null)));

                    binding.divider.setVisibility(pos == count - 1 ? View.GONE : View.VISIBLE);
                });

        getBinding().rvBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvBudgets.setAdapter(budgetAdapter);

        getBinding().rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvCategories.setAdapter(categoryAdapter);
    }

    private void observeViewModel() {
        viewModel.getBudgets().observe(getViewLifecycleOwner(), budgets -> {
            budgetAdapter.submitList(budgets);
            getBinding().cardEmptyBudgets.setVisibility(budgets.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> categoryAdapter.submitList(categories));
    }

    private void showAddBudgetBottomSheet() {
        AddBudgetBottomSheet.newInstance().show(getChildFragmentManager(), "AddBudget");
    }

    private void showAddCategoryBottomSheet() {
        AddCategoryBottomSheet.newInstance().show(getChildFragmentManager(), "AddCategory");
    }
}
