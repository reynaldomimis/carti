package com.upreyvan.carti.ui.common;

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

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.FragmentQuickLogsBottomSheetBinding;
import com.upreyvan.carti.models.Member;
import com.upreyvan.carti.utils.StringHelper;
import com.upreyvan.carti.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;

public class QuickLogsBottomSheetFragment extends BaseBottomSheetFragment<FragmentQuickLogsBottomSheetBinding> {

    public enum LogType {
        EXPENSE, DEBT, GOAL
    }

    private String selectedCategory;
    private LogType logType = LogType.EXPENSE;
    private QuickLogsViewModel viewModel;

    public static QuickLogsBottomSheetFragment newInstance(String categoryName) {
        QuickLogsBottomSheetFragment fragment = new QuickLogsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("category_name", categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedCategory = getArguments().getString("category_name");
            determineLogType();
        }
    }

    private void determineLogType() {
        if (selectedCategory == null) {
            logType = LogType.EXPENSE;
            return;
        }

        String lowerCat = selectedCategory.toLowerCase();
        if (lowerCat.contains("debt") || lowerCat.contains("utang")) {
            logType = LogType.DEBT;
        } else if (lowerCat.contains("goal") || lowerCat.contains("savings") || lowerCat.contains("ipon")) {
            logType = LogType.GOAL;
        } else {
            logType = LogType.EXPENSE;
        }
    }

    @Override
    protected FragmentQuickLogsBottomSheetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentQuickLogsBottomSheetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(QuickLogsViewModel.class);
        if (selectedCategory != null) viewModel.setCategory(selectedCategory);
        
        setupCategoryDropdown();
        setupInputValidation();
        setupClickListeners();
        observeViewModel();
        updateUI();

        if (selectedCategory != null && logType == LogType.EXPENSE) {
            getBinding().layoutForm.etCategory.setText(selectedCategory, false);
            updateSubCategoryDropdown(selectedCategory);
        }
        
        validateForm();
    }

    private void setupInputValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateForm(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        getBinding().layoutForm.etAmount.addTextChangedListener(new com.upreyvan.carti.utils.AmountTextWatcher(getBinding().layoutForm.etAmount));
        getBinding().layoutForm.etAmount.addTextChangedListener(validationWatcher);
        getBinding().layoutForm.etCategory.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        if (getBinding() == null || getBinding().layoutForm == null) return;

        String category = getBinding().layoutForm.etCategory.getText().toString().trim();
        String amountStr = getBinding().layoutForm.etAmount.getText().toString().trim();
        double amount = StringHelper.parseDouble(amountStr);
        
        boolean isAmountValid = !amountStr.isEmpty() && amount > 0;
        boolean isCategoryValid = (logType == LogType.GOAL);

        if (logType == LogType.EXPENSE || logType == LogType.DEBT) {
            isCategoryValid = !category.isEmpty();
        }

        Double currentBalance = viewModel.getRemainingBalance().getValue();
        boolean hasEnoughBalance = true;
        
        if (logType == LogType.EXPENSE && currentBalance != null && amount > currentBalance) {
            getBinding().layoutForm.tilAmount.setError(getString(R.string.err_insufficient_balance));
            hasEnoughBalance = false;
        } else if (!amountStr.isEmpty() && amount <= 0) {
            getBinding().layoutForm.tilAmount.setError(getString(R.string.msg_invalid_amount));
            hasEnoughBalance = false;
        } else {
            getBinding().layoutForm.tilAmount.setError(null);
        }

        boolean isLoading = viewModel.getIsLoading().getValue() != null && viewModel.getIsLoading().getValue();
        boolean isFormValid = isCategoryValid && isAmountValid && hasEnoughBalance;
        
        getBinding().btnSave.setEnabled(isFormValid && !isLoading);
    }

    private void setupCategoryDropdown() {
        if (logType == LogType.DEBT) {
            getBinding().layoutForm.labelCategory.setVisibility(View.VISIBLE);
            getBinding().layoutForm.layoutCategory.setVisibility(View.VISIBLE);
            getBinding().layoutForm.labelCategory.setText("WHO BORROWED?");
            viewModel.getMembers().observe(getViewLifecycleOwner(), members -> {
                List<String> names = new ArrayList<>();
                for (Member m : members) names.add(m.getTitle());
                if (names.isEmpty()) names.add("Self");
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, names);
                getBinding().layoutForm.etCategory.setAdapter(adapter);
                if (!names.isEmpty()) getBinding().layoutForm.etCategory.setText(names.get(0), false);
            });
            return;
        }

        if (logType == LogType.GOAL) {
            getBinding().layoutForm.labelCategory.setVisibility(View.GONE);
            getBinding().layoutForm.layoutCategory.setVisibility(View.GONE);
            return;
        }

        // For EXPENSE: Hide category if it was pre-selected (Direct Quick Log), show if null (General Add)
        if (selectedCategory != null) {
            getBinding().layoutForm.labelCategory.setVisibility(View.GONE);
            getBinding().layoutForm.layoutCategory.setVisibility(View.GONE);
            return;
        }

        getBinding().layoutForm.labelCategory.setVisibility(View.VISIBLE);
        getBinding().layoutForm.layoutCategory.setVisibility(View.VISIBLE);
        getBinding().layoutForm.labelCategory.setText("CATEGORY");

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
        getBinding().layoutForm.etCategory.setAdapter(adapter);
        
        getBinding().layoutForm.etCategory.setOnItemClickListener((parent, v, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            selectedCategory = selected;
            viewModel.setCategory(selected);
            updateSubCategoryDropdown(selected);
            
            // Show balance info when category is selected
            getBinding().layoutForm.allocatedHeader.setText(getString(R.string.category_expense_label, selected));
            getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE);
            validateForm();
        });
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
            getBinding().layoutForm.labelSubCategory.setVisibility(View.VISIBLE);
            getBinding().layoutForm.layoutSubCategory.setVisibility(View.VISIBLE);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subCategoryNames);
            getBinding().layoutForm.etSubCategory.setAdapter(adapter);
            getBinding().layoutForm.etSubCategory.setText(""); 

            getBinding().layoutForm.etSubCategory.setOnItemClickListener((parent, v, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                viewModel.setCategory(selected);
                getBinding().layoutForm.allocatedHeader.setText(getString(R.string.category_expense_label, selected));
                validateForm();
            });
        } else {
            getBinding().layoutForm.labelSubCategory.setVisibility(View.GONE);
            getBinding().layoutForm.layoutSubCategory.setVisibility(View.GONE);
            getBinding().layoutForm.etSubCategory.setText("");
        }
    }

    private void observeViewModel() {
        viewModel.getRemainingBalance().observe(getViewLifecycleOwner(), balance -> {
            if (getBinding() != null && getBinding().layoutForm != null) {
                if (balance != null && (logType == LogType.EXPENSE || logType == LogType.GOAL)) {
                    getBinding().layoutForm.tvAllocatedBalance.setText(StringHelper.formatCurrency(balance));
                    getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE);
                }
                validateForm();
            }
        });
        
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                String successMsg = getString(R.string.msg_save_success);
                if (logType == LogType.EXPENSE) successMsg = getString(R.string.msg_expense_saved);
                else if (logType == LogType.DEBT) successMsg = getString(R.string.msg_debt_saved_simple);
                else if (logType == LogType.GOAL) successMsg = getString(R.string.msg_goal_updated);
                showToast(successMsg, UiHelper.Status.SUCCESS);
                dismiss();
            }
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) showToast(getString(R.string.err_failed_save, error), UiHelper.Status.ERROR);
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            String msg = getString(R.string.msg_saving);
            if (logType == LogType.EXPENSE) msg = getString(R.string.msg_saving_expense);
            else if (logType == LogType.DEBT) msg = getString(R.string.msg_saving_debt);
            else if (logType == LogType.GOAL) msg = getString(R.string.msg_saving_goal);
            showLoading(loading, msg);
            validateForm();
        });
    }

    private void updateUI() {
        String title = getString(R.string.quick_log_title);
        String btnText = getString(R.string.label_save);
        
        getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE); // Always show balance info if possible

        switch (logType) {
            case DEBT:
                title = getString(R.string.title_quick_debt_log);
                getBinding().layoutForm.labelNote.setText("NOTE");
                getBinding().layoutForm.tilDescription.setHint(getString(R.string.label_note_optional));
                getBinding().layoutForm.cvBalanceInfo.setVisibility(View.GONE);
                break;
            case GOAL:
                title = getString(R.string.title_quick_goal_log);
                getBinding().layoutForm.labelNote.setText("NOTE");
                getBinding().layoutForm.tilDescription.setHint(getString(R.string.label_note_optional));
                getBinding().layoutForm.allocatedHeader.setText(getString(R.string.label_goal_progress));
                break;
            case EXPENSE:
                title = getString(R.string.quick_log_title);
                getBinding().layoutForm.labelNote.setText("NOTE");
                getBinding().layoutForm.tilDescription.setHint("Note (Optional)");
                
                if (selectedCategory != null) {
                    getBinding().layoutForm.allocatedHeader.setText(getString(R.string.category_expense_label, selectedCategory));
                } else {
                    getBinding().layoutForm.allocatedHeader.setText(getString(R.string.label_budget_plan_allocation));
                }
                break;
        }

        getBinding().tvCategoryLabel.setText(title);
        getBinding().btnSave.setText(btnText);
    }

    private void setupClickListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());
        getBinding().btnSave.setOnClickListener(v -> {
            if (!checkNetwork()) return;

            String categoryToSave = getBinding().layoutForm.etCategory.getText().toString().trim();
            String subCategoryToSave = getBinding().layoutForm.etSubCategory.getText().toString().trim();

            double amountVal = StringHelper.parseDouble(getBinding().layoutForm.etAmount.getText().toString());
            String description = getBinding().layoutForm.etDescription.getText().toString();
            
            String finalCategory = (logType == LogType.EXPENSE && !subCategoryToSave.isEmpty()) ? subCategoryToSave : categoryToSave;
            if (finalCategory.isEmpty()) finalCategory = selectedCategory;

            switch (logType) {
                case EXPENSE:
                    viewModel.saveTrack(amountVal, finalCategory, description, "Cash");
                    break;
                case DEBT:
                    viewModel.saveDebt(amountVal, categoryToSave, description);
                    break;
                case GOAL:
                    viewModel.saveGoal(amountVal, selectedCategory);
                    break;
            }
        });
    }
}
