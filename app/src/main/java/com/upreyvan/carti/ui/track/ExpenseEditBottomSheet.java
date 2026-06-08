package com.upreyvan.carti.ui.track;

import android.os.Bundle;
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
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.DialogEditExpenseBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.UiHelper;

import java.util.List;
import java.util.Map;

public class ExpenseEditBottomSheet extends BaseBottomSheetFragment<DialogEditExpenseBinding> {

    private Transaction transaction;
    private TransactionRepository transactionRepository;
    private OnExpenseUpdatedListener listener;

    public interface OnExpenseUpdatedListener {
        void onExpenseUpdated();
    }

    public static ExpenseEditBottomSheet newInstance(Transaction transaction) {
        ExpenseEditBottomSheet fragment = new ExpenseEditBottomSheet();
        fragment.transaction = transaction;
        return fragment;
    }

    public void setListener(OnExpenseUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    protected DialogEditExpenseBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return DialogEditExpenseBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        setupUI();
        setupInputValidation();
        getBinding().btnSave.setOnClickListener(v -> handleSave());
        validateForm();
    }

    private void setupInputValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateForm(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        getBinding().etDescription.addTextChangedListener(validationWatcher);
        getBinding().etAmount.addTextChangedListener(new com.upreyvan.carti.util.AmountTextWatcher(getBinding().etAmount));
        getBinding().etAmount.addTextChangedListener(validationWatcher);
        getBinding().actvCategory.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        String description = getBinding().etDescription.getText().toString().trim();
        String amountStr = getBinding().etAmount.getText().toString().trim();
        
        double amount = com.upreyvan.carti.util.StringHelper.parseDouble(amountStr);
        
        boolean isValid = !description.isEmpty() && amount > 0;
        
        getBinding().btnSave.setEnabled(isValid);
    }

    private void setupUI() {
        if (transaction == null) return;

        getBinding().etAmount.setText(String.valueOf(transaction.getAmount()));
        getBinding().etDescription.setText(transaction.getTitle());

        List<Category> cats = CategoryManager.getInstance(requireContext()).getCategories();
        String[] names = cats.stream().map(Category::getName).toArray(String[]::new);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
        getBinding().actvCategory.setAdapter(adapter);
        getBinding().actvCategory.setText(transaction.getCategory(), false);
    }

    private void handleSave() {
        if (!checkNetwork()) return;

        try {
            double amount = com.upreyvan.carti.util.StringHelper.parseDouble(getBinding().etAmount.getText().toString().trim());
            String title = getBinding().etDescription.getText().toString().trim();
            String category = getBinding().actvCategory.getText().toString().trim();

            setLoading(true);
            transaction.setAmount(amount);
            transaction.setTitle(title);
            transaction.setCategory(category);

            transactionRepository.updateTransaction(transaction, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    onActionSuccess();
                }

                @Override
                public void onError(Throwable error) {
                    onActionError(error);
                }
            });

        } catch (NumberFormatException e) {
            showToast(R.string.msg_invalid_amount, UiHelper.Status.ERROR);
        }
    }

    private void onActionSuccess() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            showToast(R.string.msg_save_success, UiHelper.Status.SUCCESS);
            if (listener != null) listener.onExpenseUpdated();
            dismiss();
        });
    }

    private void onActionError(Throwable error) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            setLoading(false);
            showToast("Error: " + error.getMessage(), UiHelper.Status.ERROR);
        });
    }

    private void setLoading(boolean loading) {
        if (!loading) validateForm();
        else getBinding().btnSave.setEnabled(false);
        getBinding().btnSave.setText(loading ? R.string.label_saving : R.string.label_update);
    }
}
