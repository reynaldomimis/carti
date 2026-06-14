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
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.BudgetCategoryItem;
import com.upreyvan.carti.utils.DialogHelper;
import com.upreyvan.carti.utils.Utils;
import java.util.ArrayList;
import java.util.Calendar;

public class BudgetFragment extends BaseFragment<FragmentBudgetBinding> {
    private PlanViewModel viewModel;
    private GenericAdapter<BudgetCategoryItem, ItemBudgetCardBinding> budgetAdapter;
    private GenericAdapter<BudgetCategoryItem, ItemCategoryRowBinding> categoryAdapter;
    private boolean isCategoriesExpanded = false;
    private boolean isBudgetsExpanded = false;
    private java.util.List<BudgetCategoryItem> fullBudgets = new java.util.ArrayList<>();
    private java.util.List<BudgetCategoryItem> fullParentCategories = new java.util.ArrayList<>();

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
        setupDynamicPadding(null, getBinding().rootScroll);
        observeViewModel();
    }

    private void setupUI() {
        getBinding().layoutHeader.tvHeaderTitle.setText(R.string.budget_setup_header);
        String monthYear = Utils.formatMonthYear(Calendar.getInstance());
        getBinding().layoutHeader.tvHeaderSubtitle.setText(getString(R.string.for_the_month_of, monthYear));
        getBinding().layoutHeader.btnHeaderAction.setOnClickListener(v -> showAddBudgetBottomSheet());

        getBinding().btnAddCategory.setOnClickListener(v -> showAddCategoryBottomSheet());
        getBinding().btnViewMore.setOnClickListener(v -> {
            isCategoriesExpanded = true;
            updateCategoryList();
        });

        getBinding().btnViewMoreBudgets.setOnClickListener(v -> {
            isBudgetsExpanded = true;
            updateBudgetList();
        });
    }

    private void setupAdapters() {
        budgetAdapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemBudgetCardBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.tvRemainingText.setText(Utils.formatCurrency(Math.max(0, item.getAmount() - item.getCurrentSpent())));
                    binding.tvStatus.setText(getString(R.string.label_balance));
                    
                    com.upreyvan.carti.utils.UiHelper.applyCategoryStyle(binding.getRoot(), null, null, item.getCategoryName());
                    binding.getRoot().setOnClickListener(v -> showEditBudgetBottomSheet(item));
                });

        categoryAdapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemCategoryRowBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.ivCategoryIcon.setImageResource(item.getIconRes() != 0 ? item.getIconRes() : R.drawable.ic_chart);
                    
                    com.upreyvan.carti.utils.UiHelper.applyCategoryStyle(null, binding.cardIcon, binding.ivCategoryIcon, item.getCategoryName());
                    binding.btnOptions.setOnClickListener(v -> showCategoryOptions(item, v));
                });

        getBinding().rvBudgets.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3));
        getBinding().rvBudgets.setAdapter(budgetAdapter);
        getBinding().rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvCategories.setAdapter(categoryAdapter);
    }

    private void observeViewModel() {
        viewModel.getBudgets().observe(getViewLifecycleOwner(), budgets -> {
            if (getBinding().shimmerBudgets.isShimmerStarted()) {
                getBinding().shimmerBudgets.stopShimmer();
                getBinding().shimmerBudgets.setVisibility(View.GONE);
                getBinding().rvBudgets.setVisibility(View.VISIBLE);
            }

            fullBudgets = new ArrayList<>(budgets);
            Utils.sortAlphabetically(fullBudgets, BudgetCategoryItem::getCategoryName);
            updateBudgetList();
            getBinding().cardEmptyBudgets.setVisibility(budgets.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            fullParentCategories = categories.stream()
                    .filter(c -> c.getParentCategory() == null || c.getParentCategory().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            Utils.sortAlphabetically(fullParentCategories, BudgetCategoryItem::getCategoryName);
            updateCategoryList();
        });
    }

    private void updateCategoryList() {
        if (!isCategoriesExpanded && fullParentCategories.size() > 9) {
            categoryAdapter.submitList(new java.util.ArrayList<>(fullParentCategories.subList(0, 9)));
            getBinding().btnViewMore.setVisibility(View.VISIBLE);
        } else {
            categoryAdapter.submitList(new java.util.ArrayList<>(fullParentCategories));
            getBinding().btnViewMore.setVisibility(View.GONE);
        }
    }

    private void updateBudgetList() {
        if (!isBudgetsExpanded && fullBudgets.size() > 9) {
            budgetAdapter.submitList(new java.util.ArrayList<>(fullBudgets.subList(0, 9)));
            getBinding().btnViewMoreBudgets.setVisibility(View.VISIBLE);
        } else {
            budgetAdapter.submitList(new java.util.ArrayList<>(fullBudgets));
            getBinding().btnViewMoreBudgets.setVisibility(View.GONE);
        }
    }

    private void showAddBudgetBottomSheet() {
        AddBudgetBottomSheet.newInstance().show(getChildFragmentManager(), "AddBudget");
    }

    private void showEditBudgetBottomSheet(BudgetCategoryItem item) {
        AddBudgetBottomSheet.newInstance(item).show(getChildFragmentManager(), "EditBudget");
    }

    private void showAddCategoryBottomSheet() {
        AddCategoryBottomSheet.newInstance().show(getChildFragmentManager(), "AddCategory");
    }

    private void showSubCategories(BudgetCategoryItem item) {
        SubCategoriesBottomSheet.newInstance(item.getCategoryName())
                .show(getChildFragmentManager(), "SubCategories");
    }

    private void showCategoryOptions(BudgetCategoryItem item, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_category_options, popup.getMenu());
        
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_show_sub) {
                showSubCategories(item);
                return true;
            } else if (id == R.id.action_edit) {
                AddCategoryBottomSheet.newInstance(item)
                        .show(getChildFragmentManager(), "EditCategory");
                return true;
            } else if (id == R.id.action_delete) {
                confirmDeleteCategory(item);
                return true;
            }
            return false;
        });
        com.upreyvan.carti.utils.UiHelper.showPopupMenuWithIcons(popup);
    }

    private void confirmDeleteCategory(BudgetCategoryItem item) {
        DialogHelper.showConfirmation(requireContext(),
                "Delete Category?",
                "Are you sure you want to delete '" + item.getCategoryName() + "'? This will also delete all its sub-categories.",
                "Delete",
                () -> {
                    com.upreyvan.carti.managers.CategoryManager manager = com.upreyvan.carti.managers.CategoryManager.getInstance(requireContext());
                    manager.deleteCategory(item.getCategoryName());
                    TransactionRepository.getInstance(requireContext()).deleteCategory(item.getCategoryName());
                    
                    // Remote Delete if it has an ID
                    if (item.getCategoryName().length() > 20) {
                        manager.deleteCategoryRemote(item.getCategoryName());
                    }

                    viewModel.loadData();
                }
        );
    }
}
