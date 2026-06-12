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
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.databinding.BottomSheetSubCategoriesBinding;
import com.upreyvan.carti.databinding.ItemCategoryRowBinding;
import com.upreyvan.carti.models.BudgetCategoryItem;
import com.upreyvan.carti.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SubCategoriesBottomSheet extends BaseBottomSheetFragment<BottomSheetSubCategoriesBinding> {
    private static final String ARG_PARENT_CATEGORY = "parent_category";
    private String parentCategory;
    private PlanViewModel viewModel;
    private GenericAdapter<BudgetCategoryItem, ItemCategoryRowBinding> adapter;

    public static SubCategoriesBottomSheet newInstance(String parentCategory) {
        SubCategoriesBottomSheet fragment = new SubCategoriesBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PARENT_CATEGORY, parentCategory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            parentCategory = getArguments().getString(ARG_PARENT_CATEGORY);
        }
    }

    @Override
    protected BottomSheetSubCategoriesBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetSubCategoriesBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlanViewModel.class);
        
        setupAdapter();
        observeViewModel();
        
        getBinding().tvTitle.setText(getString(R.string.title_sub_categories_for, parentCategory));
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnAddSub.setOnClickListener(v -> {
            AddCategoryBottomSheet addBs = AddCategoryBottomSheet.newInstance(parentCategory);
            addBs.show(getChildFragmentManager(), "AddSubCategory");
        });
    }

    private void setupAdapter() {
        adapter = new GenericAdapter<>(BudgetCategoryItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemCategoryRowBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvCategoryName.setText(item.getCategoryName());
                    binding.ivCategoryIcon.setImageResource(item.getIconRes() != 0 ? item.getIconRes() : R.drawable.ic_chart);
                    
                    com.upreyvan.carti.utils.UiHelper.applyCategoryStyle(null, binding.cardIcon, binding.ivCategoryIcon, item.getCategoryName());

                    binding.btnOptions.setVisibility(View.VISIBLE);
                    binding.btnOptions.setOnClickListener(v -> showSubCategoryOptions(item, v));
                });

        getBinding().rvSubCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvSubCategories.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), allCategories -> {
            List<BudgetCategoryItem> subs = allCategories.stream()
                    .filter(c -> parentCategory != null && parentCategory.equalsIgnoreCase(c.getParentCategory()))
                    .collect(Collectors.toList());
            
            adapter.submitList(new ArrayList<>(subs));
            getBinding().tvEmpty.setVisibility(subs.isEmpty() ? View.VISIBLE : View.GONE);
            getBinding().rvSubCategories.setVisibility(subs.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void showSubCategoryOptions(BudgetCategoryItem item, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_category_options, popup.getMenu());
        
        // Hide "Show Sub-categories" as we are already viewing subs
        popup.getMenu().findItem(R.id.action_show_sub).setVisible(false);

        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_edit) {
                AddCategoryBottomSheet.newInstance(item)
                        .show(getChildFragmentManager(), "EditSubCategory");
                return true;
            } else if (id == R.id.action_delete) {
                confirmDeleteSubCategory(item);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteSubCategory(BudgetCategoryItem item) {
        DialogHelper.showConfirmation(requireContext(),
                "Delete Sub-category?",
                "Are you sure you want to delete '" + item.getCategoryName() + "'?",
                "Delete",
                () -> {
                    com.upreyvan.carti.managers.CategoryManager.getInstance(requireContext()).deleteCategory(item.getCategoryName());
                    TransactionRepository.getInstance(requireContext()).deleteCategory(item.getCategoryName());
                    viewModel.loadData();
                }
        );
    }
}
