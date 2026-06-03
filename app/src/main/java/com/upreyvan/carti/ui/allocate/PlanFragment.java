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
        
        viewModel.loadData();

        selectTab(R.id.tabBudget);
    }


    private void setupUI() {
        String monthYear = Utils.formatMonthYear(Calendar.getInstance());
        getBinding().tvCurrentMonth.setText(getString(R.string.for_the_month_of, monthYear));

        getBinding().tabBudget.setOnClickListener(v -> selectTab(v.getId()));
        getBinding().tabBills.setOnClickListener(v -> selectTab(v.getId()));
        getBinding().tabGoals.setOnClickListener(v -> selectTab(v.getId()));

        getBinding().btnAddBudget.setOnClickListener(v -> showAddBudgetBottomSheet());
        getBinding().btnAddCategory.setOnClickListener(v -> showAddCategoryBottomSheet());
    }

    private void selectTab(int id) {
        updateTabStyle(getBinding().tabBudget, id == R.id.tabBudget);
        updateTabStyle(getBinding().tabBills, id == R.id.tabBills);
        updateTabStyle(getBinding().tabGoals, id == R.id.tabGoals);
    }

    private void updateTabStyle(com.google.android.material.button.MaterialButton tab, boolean isActive) {
        int activeBg = getResources().getColor(R.color.white, null);
        int inactiveBg = getResources().getColor(android.R.color.transparent, null);
        int activeContent = getResources().getColor(R.color.carti_primary_green, null);
        int inactiveContent = getResources().getColor(R.color.text_secondary, null);

        tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isActive ? activeBg : inactiveBg));
        tab.setTextColor(isActive ? activeContent : inactiveContent);
        tab.setIconTint(android.content.res.ColorStateList.valueOf(isActive ? activeContent : inactiveContent));

        if (isActive) {
            tab.setElevation(2f);
        } else {
            tab.setElevation(0f);
        }
    }

    private void setupAdapters() {
        budgetAdapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemBudgetCardBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.tvRemainingText.setText(Utils.formatCurrency(Math.max(0, item.getAmount() - item.getCurrentSpent())));
                    binding.tvStatus.setText(item.getAmount() - item.getCurrentSpent() >= 0 ? "left" : "over");
                });

        categoryAdapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemCategoryRowBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.ivCategoryIcon.setImageResource(item.getIconRes() != 0 ? item.getIconRes() : R.drawable.ic_chart);
                    
                    int bgColor = item.getBgColor() != 0 ? item.getBgColor() : R.color.mint_green_alpha;
                    int iconColor = item.getIconColor() != 0 ? item.getIconColor() : R.color.carti_primary_green;
                    
                    binding.cardIcon.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(getResources().getColor(bgColor, null)));
                    binding.ivCategoryIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(iconColor, null)));
                });

        getBinding().rvBudgets.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3));
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
