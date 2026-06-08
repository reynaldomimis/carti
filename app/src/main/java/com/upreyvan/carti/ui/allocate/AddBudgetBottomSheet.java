package com.upreyvan.carti.ui.allocate;

import android.os.Bundle;
import android.text.Editable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.BottomSheetAddBudgetBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class AddBudgetBottomSheet extends BaseBottomSheetFragment<BottomSheetAddBudgetBinding> {

    private BudgetCategoryItem editingItem;

    public static AddBudgetBottomSheet newInstance() {
        return new AddBudgetBottomSheet();
    }

    public static AddBudgetBottomSheet newInstance(BudgetCategoryItem item) {
        AddBudgetBottomSheet fragment = new AddBudgetBottomSheet();
        fragment.editingItem = item;
        return fragment;
    }

    @Override
    protected BottomSheetAddBudgetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetAddBudgetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        setupCategoryDropdown();
        
        getBinding().btnClose.setOnClickListener(v1 -> dismiss());
        getBinding().btnCancel.setOnClickListener(v1 -> dismiss());
        getBinding().btnSave.setOnClickListener(v1 -> saveBudget());

        setupInputValidation();

        if (editingItem != null) {
            getBinding().tvTitle.setText(R.string.btn_edit);
            getBinding().etCategory.setText(editingItem.getCategoryName(), false);
            getBinding().etCategory.setEnabled(false); // Usually don't want to change category name here
            getBinding().etLimit.setText(String.valueOf(editingItem.getAmount()));
            getBinding().switchRecurring.setChecked(editingItem.isRecurring());
        }

        getBinding().etCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = (String) parent.getItemAtPosition(position);
            updateSubCategoryDropdown(selectedCategory);
            validateForm();
        });
        
        validateForm();
    }

    private void setupInputValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateForm(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        getBinding().etCategory.addTextChangedListener(validationWatcher);
        getBinding().etLimit.addTextChangedListener(new com.upreyvan.carti.util.AmountTextWatcher(getBinding().etLimit));
        getBinding().etLimit.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        String category = getBinding().etCategory.getText().toString().trim();
        String limitStr = getBinding().etLimit.getText().toString().trim();
        
        double limit = com.upreyvan.carti.util.StringHelper.parseDouble(limitStr);
        
        boolean isValid = !category.isEmpty() && limit > 0;
        
        getBinding().btnSave.setEnabled(isValid);
    }

    private void updateSubCategoryDropdown(String parentCategoryName) {
        List<com.upreyvan.carti.model.Category> allCategories = com.upreyvan.carti.data.local.CategoryManager.getInstance(requireContext()).getCategories();
        List<String> subCategoryNames = new ArrayList<>();
        for (com.upreyvan.carti.model.Category item : allCategories) {
            if (parentCategoryName.equalsIgnoreCase(item.getParentCategory())) {
                subCategoryNames.add(item.getName());
            }
        }

        if (!subCategoryNames.isEmpty()) {
            getBinding().labelSubCategory.setVisibility(View.VISIBLE);
            getBinding().layoutSubCategory.setVisibility(View.VISIBLE);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subCategoryNames);
            getBinding().etSubCategory.setAdapter(adapter);
            getBinding().etSubCategory.setText(""); // Reset sub-category when parent changes
        } else {
            getBinding().labelSubCategory.setVisibility(View.GONE);
            getBinding().layoutSubCategory.setVisibility(View.GONE);
            getBinding().etSubCategory.setText("");
        }
    }

    private void setupCategoryDropdown() {
        // Load from CategoryManager to include all defined categories (even those without budgets)
        List<com.upreyvan.carti.model.Category> categories = com.upreyvan.carti.data.local.CategoryManager.getInstance(requireContext()).getCategories();
        List<String> names = new ArrayList<>();
        for (com.upreyvan.carti.model.Category item : categories) {
            // Only show parent categories in the main budget dropdown
            if (item.getParentCategory() == null || item.getParentCategory().isEmpty()) {
                names.add(item.getName());
            }
        }
        
        names.sort((a, b) -> {
            if (a.equalsIgnoreCase("Others")) return 1;
            if (b.equalsIgnoreCase("Others")) return -1;
            return a.compareToIgnoreCase(b);
        });
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
        getBinding().etCategory.setAdapter(adapter);
    }

    private void saveBudget() {
        Editable categoryText = getBinding().etCategory.getText();
        Editable subCategoryText = getBinding().etSubCategory.getText();
        Editable amountText = getBinding().etLimit.getText();

        if (categoryText == null || amountText == null) return;

        String category = categoryText.toString().trim();
        String subCategory = subCategoryText != null ? subCategoryText.toString().trim() : "";
        String amountStr = amountText.toString().trim();
        boolean isRecurring = getBinding().switchRecurring.isChecked();
        
        // Determine which category name to use for the budget record.
        // If a sub-category is selected, we use the sub-category name but keep the link to the parent.
        String targetCategory = subCategory.isEmpty() ? category : subCategory;
        String parentName = subCategory.isEmpty() ? null : category;
        
        try {
            double amount = com.upreyvan.carti.util.StringHelper.parseDouble(amountStr);
            TransactionRepository.getInstance(requireContext()).updateOrAddCategory(targetCategory, amount, parentName, isRecurring);
            dismiss();
        } catch (NumberFormatException e) {
            showToast(R.string.msg_invalid_amount, UiHelper.Status.ERROR);
        }
    }
}
