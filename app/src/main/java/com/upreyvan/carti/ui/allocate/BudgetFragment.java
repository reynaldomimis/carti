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
import com.upreyvan.carti.databinding.FragmentBudgetBinding;
import com.upreyvan.carti.databinding.ItemBudgetCardBinding;
import com.upreyvan.carti.databinding.ItemCategoryRowBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;

public class BudgetFragment extends BaseFragment<FragmentBudgetBinding> {
    private PlanViewModel viewModel;
    private GenericAdapter<BudgetCategoryItem, ItemBudgetCardBinding> budgetAdapter;
    private GenericAdapter<BudgetCategoryItem, ItemCategoryRowBinding> categoryAdapter;

    @Override
    protected FragmentBudgetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentBudgetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlanViewModel.class);
        setupUI();
        setupAdapters();
        observeViewModel();
    }

    private void setupUI() {
        getBinding().layoutHeader.tvHeaderTitle.setText(R.string.budget_setup_header);
        String monthYear = Utils.formatMonthYear(Calendar.getInstance());
        getBinding().layoutHeader.tvHeaderSubtitle.setText(getString(R.string.for_the_month_of, monthYear));
        getBinding().layoutHeader.btnHeaderAction.setOnClickListener(v -> showAddBudgetBottomSheet());

        getBinding().btnAddCategory.setOnClickListener(v -> showAddCategoryBottomSheet());
    }

    private void setupAdapters() {
        budgetAdapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemBudgetCardBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.tvRemainingText.setText(Utils.formatCurrency(Math.max(0, item.getAmount() - item.getCurrentSpent())));
                    binding.tvStatus.setText(getString(R.string.label_balance));
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
                });

        getBinding().rvBudgets.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3));
        getBinding().rvBudgets.setAdapter(budgetAdapter);
        getBinding().rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvCategories.setAdapter(categoryAdapter);
    }

    private void observeViewModel() {
        viewModel.getBudgets().observe(getViewLifecycleOwner(), budgets -> {
            budgetAdapter.submitList(new ArrayList<>(budgets));
            getBinding().cardEmptyBudgets.setVisibility(budgets.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> categoryAdapter.submitList(new ArrayList<>(categories)));
    }

    private void showAddBudgetBottomSheet() {
        AddBudgetBottomSheet.newInstance().show(getChildFragmentManager(), "AddBudget");
    }

    private void showAddCategoryBottomSheet() {
        AddCategoryBottomSheet.newInstance().show(getChildFragmentManager(), "AddCategory");
    }
}
