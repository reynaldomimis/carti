package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.BottomSheetContributeBinding;
import com.upreyvan.carti.utils.AmountTextWatcher;
import com.upreyvan.carti.utils.StringHelper;
import com.upreyvan.carti.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;

public class ContributeBottomSheetFragment extends BaseBottomSheetFragment<BottomSheetContributeBinding> {

    private MembersViewModel viewModel;

    public static ContributeBottomSheetFragment newInstance() {
        return new ContributeBottomSheetFragment();
    }

    @Override
    protected BottomSheetContributeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetContributeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(MembersViewModel.class);

        setupCategoryDropdown();
        setupListeners();
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());

        getBinding().etAmount.addTextChangedListener(new AmountTextWatcher(getBinding().etAmount));
        getBinding().etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validate(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        getBinding().etCategory.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validate(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        getBinding().etCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = (String) parent.getItemAtPosition(position);
            updateSubCategoryDropdown(selectedCategory);
            validate();
        });

        getBinding().btnContribute.setOnClickListener(v -> {
            if (!checkNetwork()) return;
            
            String category = getBinding().etCategory.getText().toString().trim();
            String subCategory = getBinding().etSubCategory.getText().toString().trim();
            double amount = StringHelper.parseDouble(getBinding().etAmount.getText().toString());
            
            saveContribution(amount, category, subCategory);
        });
    }

    private void setupCategoryDropdown() {
        List<com.upreyvan.carti.models.Category> categories = com.upreyvan.carti.managers.CategoryManager.getInstance(requireContext()).getCategories();
        List<String> names = new ArrayList<>();
        for (com.upreyvan.carti.models.Category item : categories) {
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

    private void updateSubCategoryDropdown(String parentCategoryName) {
        List<com.upreyvan.carti.models.Category> allCategories = com.upreyvan.carti.managers.CategoryManager.getInstance(requireContext()).getCategories();
        List<String> subCategoryNames = new ArrayList<>();
        for (com.upreyvan.carti.models.Category item : allCategories) {
            if (parentCategoryName.equalsIgnoreCase(item.getParentCategory())) {
                subCategoryNames.add(item.getName());
            }
        }

        if (!subCategoryNames.isEmpty()) {
            getBinding().labelSubCategory.setVisibility(View.VISIBLE);
            getBinding().layoutSubCategory.setVisibility(View.VISIBLE);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subCategoryNames);
            getBinding().etSubCategory.setAdapter(adapter);
            getBinding().etSubCategory.setText(""); 
        } else {
            getBinding().labelSubCategory.setVisibility(View.GONE);
            getBinding().layoutSubCategory.setVisibility(View.GONE);
            getBinding().etSubCategory.setText("");
        }
    }

    private void validate() {
        String amountStr = getBinding().etAmount.getText().toString().trim();
        String category = getBinding().etCategory.getText().toString().trim();
        double amount = StringHelper.parseDouble(amountStr);
        
        boolean isAmountEntered = !amountStr.isEmpty();
        boolean isCategoryValid = !category.isEmpty();
        boolean isValidAmount = amount > 0;
        
        getBinding().btnContribute.setEnabled(isAmountEntered && isCategoryValid && isValidAmount);
        
        if (isAmountEntered && amount <= 0) {
            getBinding().tilAmount.setError("Please enter a valid amount");
        } else if (!isAmountEntered) {
            getBinding().tilAmount.setError("Amount is required");
        } else {
            getBinding().tilAmount.setError(null);
            getBinding().tilAmount.setErrorEnabled(false);
        }
        
        if (!category.isEmpty()) {
            getBinding().layoutCategory.setError(null);
            getBinding().layoutCategory.setErrorEnabled(false);
        }
    }

    private void saveContribution(double amount, String category, String subCategory) {
        showLoading(true, "Saving contribution...");

        com.upreyvan.carti.models.Transaction t = new com.upreyvan.carti.models.Transaction();
        t.setAmount(amount);
        t.setType("ALLOCATION");
        
        if (subCategory != null && !subCategory.isEmpty()) {
            t.setCategory(category);
            t.setSubCategory(subCategory);
            t.setTitle("Contribution: " + subCategory);
        } else {
            t.setCategory(category);
            t.setSubCategory(null);
            t.setTitle("Contribution: " + category);
        }
        
        t.setNote("Manual contribution to shared budget");
        t.setAllocationMonth(com.upreyvan.carti.utils.Utils.formatMonthQuery(java.util.Calendar.getInstance()));

        com.upreyvan.carti.repository.TransactionRepository.getInstance(requireContext())
            .createItem("ALLOCATION", t,
                new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<java.util.Map<String, Object>>() {
                    @Override
                    public void onSuccess(java.util.Map<String, Object> result) {
                        showLoading(false);
                        showToast("Contribution saved successfully!", UiHelper.Status.SUCCESS);
                        viewModel.refreshData();
                        dismiss();
                    }
                    @Override
                    public void onError(Throwable error) {
                        showLoading(false);
                        String msg = error.getMessage() != null ? error.getMessage() : "Unknown error";
                        showToast("Failed to save: " + msg, UiHelper.Status.ERROR);
                    }
                });
    }
}
