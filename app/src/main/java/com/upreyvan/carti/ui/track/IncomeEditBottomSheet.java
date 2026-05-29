package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.DialogEditIncomeBinding;
import com.upreyvan.carti.model.Transaction;
import java.util.Map;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.ValueHelper;

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
        getBinding().btnSave.setOnClickListener(v -> handleSave());
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

        if (Validator.isEmpty(getBinding().etSalaryAmount)) {
            showToast("Please enter an amount", ToastHelper.Status.WARNING);
            return;
        }

        if (Validator.isEmpty(getBinding().etIncomeSource)) {
            showToast("Please enter a source", ToastHelper.Status.WARNING);
            return;
        }

        try {
            double amount = Double.parseDouble(getBinding().etSalaryAmount.getText().toString().trim());
            setLoading(true);

            if (mode == Mode.ADD_INCOME) {
                addExtraIncome(getBinding().etIncomeSource.getText().toString().trim(), amount);
            } else if (mode == Mode.EDIT_INCOME) {
                updateExtraIncome(getBinding().etIncomeSource.getText().toString().trim(), amount);
            }

        } catch (NumberFormatException e) {
            showToast("Invalid amount", ToastHelper.Status.ERROR);
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
            showToast("Error: " + error.getMessage(), ToastHelper.Status.ERROR);
        });
    }

    private void setLoading(boolean loading) {
        getBinding().btnSave.setEnabled(!loading);
        getBinding().btnSave.setText(loading ? R.string.label_saving : (mode == Mode.ADD_INCOME ? R.string.label_add_income : R.string.label_save));
    }

    protected void showToast(String message, ToastHelper.Status status) {
        ToastHelper.show(requireContext(), message, status);
    }
}
