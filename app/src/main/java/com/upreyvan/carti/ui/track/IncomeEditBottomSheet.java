package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.DialogEditIncomeBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.ValueHelper;

import java.util.Map;

public class IncomeEditBottomSheet extends BaseBottomSheetFragment<DialogEditIncomeBinding> {

    public enum Mode {
        ADD_INCOME,
        EDIT_INCOME
    }

    private OnIncomeUpdatedListener listener;
    private TransactionRepository transactionRepository;
    private Mode mode = Mode.ADD_INCOME;
    private Transaction incomeToEdit;

    public interface OnIncomeUpdatedListener {
        void onIncomeUpdated();
    }

    public static IncomeEditBottomSheet newInstance(Mode mode) {
        IncomeEditBottomSheet fragment = new IncomeEditBottomSheet();
        fragment.mode = mode;
        return fragment;
    }

    public static IncomeEditBottomSheet newInstance(Transaction income) {
        IncomeEditBottomSheet fragment = new IncomeEditBottomSheet();
        fragment.mode = Mode.EDIT_INCOME;
        fragment.incomeToEdit = income;
        return fragment;
    }

    public void setListener(OnIncomeUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    protected DialogEditIncomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return DialogEditIncomeBinding.inflate(inflater, container, false);
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

        getBinding().etIncomeSource.addTextChangedListener(validationWatcher);
        getBinding().etSalaryAmount.addTextChangedListener(new com.upreyvan.carti.util.AmountTextWatcher(getBinding().etSalaryAmount));
        getBinding().etSalaryAmount.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        String source = getBinding().etIncomeSource.getText().toString().trim();
        String amountStr = getBinding().etSalaryAmount.getText().toString().trim();
        
        double amount = com.upreyvan.carti.util.StringHelper.parseDouble(amountStr);
        
        boolean isValid = !source.isEmpty() && amount > 0;
        
        getBinding().btnSave.setEnabled(isValid);
    }

    private void setupUI() {
        switch (mode) {
            case ADD_INCOME:
                getBinding().tvDialogTitle.setText(R.string.title_add_income);
                getBinding().cardSource.setVisibility(View.VISIBLE);
                getBinding().tilIncomeSource.setHint(getString(R.string.label_title));
                getBinding().etIncomeSource.setHint(R.string.hint_income_source);
                getBinding().tilSalaryAmount.setHint(getString(R.string.label_bill_amount));
                getBinding().etSalaryAmount.setText("");
                getBinding().btnSave.setText(R.string.label_add_income);
                break;

            case EDIT_INCOME:
                getBinding().tvDialogTitle.setText(R.string.title_edit_income_extra);
                getBinding().cardSource.setVisibility(View.VISIBLE);
                getBinding().tilIncomeSource.setHint(getString(R.string.label_title));
                getBinding().tilSalaryAmount.setHint(getString(R.string.label_bill_amount));
                if (incomeToEdit != null) {
                    getBinding().etIncomeSource.setText(ValueHelper.toStr(incomeToEdit.getTitle()));
                    getBinding().etSalaryAmount.setText(String.valueOf(incomeToEdit.getAmount()));
                }
                getBinding().btnSave.setText(R.string.label_update);
                break;
        }
    }

    private void handleSave() {
        if (!checkNetwork()) return;

        try {
            double amount = com.upreyvan.carti.util.StringHelper.parseDouble(getBinding().etSalaryAmount.getText().toString().trim());
            setLoading(true);

            if (mode == Mode.ADD_INCOME) {
                addExtraIncome(getBinding().etIncomeSource.getText().toString().trim(), amount);
            } else if (mode == Mode.EDIT_INCOME) {
                updateExtraIncome(getBinding().etIncomeSource.getText().toString().trim(), amount);
            }

        } catch (NumberFormatException e) {
            showToast("Invalid amount", UiHelper.Status.ERROR);
        }
    }

    private void addExtraIncome(String source, double amount) {
        Transaction income = new Transaction();
        income.setType("INCOME");
        income.setTitle(source);
        income.setAmount(amount);
        income.setCategory("Income");
        income.setNote(source);
        income.setUserId(com.upreyvan.carti.data.local.PreferenceManager.getInstance(requireContext()).getUserId());
        income.setFamilyId(com.upreyvan.carti.data.local.PreferenceManager.getInstance(requireContext()).getFamilyId());

        transactionRepository.addTransaction(income, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                onActionSuccess("Income added");
            }

            @Override
            public void onError(Throwable error) {
                onActionError(error);
            }
        });
    }

    private void updateExtraIncome(String source, double amount) {
        if (incomeToEdit == null) return;
        incomeToEdit.setTitle(source);
        incomeToEdit.setAmount(amount);
        incomeToEdit.setCategory("Income");

        transactionRepository.updateTransaction(incomeToEdit, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                onActionSuccess("Income updated");
            }

            @Override
            public void onError(Throwable error) {
                onActionError(error);
            }
        });
    }

    private void onActionSuccess(String message) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (listener != null) listener.onIncomeUpdated();
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
        getBinding().btnSave.setText(loading ? R.string.label_saving : (mode == Mode.ADD_INCOME ? R.string.label_add_income : R.string.label_save));
    }
}
