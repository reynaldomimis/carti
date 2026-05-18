package com.upreyvan.carti.ui.expenses;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.IncomeRepository;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.DialogEditIncomeBinding;
import com.upreyvan.carti.model.Income;

import java.util.Map;

import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.util.Validator;

public class IncomeEditBottomSheet extends BaseBottomSheetFragment<DialogEditIncomeBinding> {

    public enum Mode {
        ADD_INCOME,
        EDIT_INCOME
    }

    private OnIncomeUpdatedListener listener;
    private MemberRepository memberRepository;
    private IncomeRepository incomeRepository;
    private Mode mode = Mode.ADD_INCOME;
    private Income incomeToEdit;

    public interface OnIncomeUpdatedListener {
        void onIncomeUpdated();
    }

    public static IncomeEditBottomSheet newInstance(Mode mode) {
        IncomeEditBottomSheet fragment = new IncomeEditBottomSheet();
        fragment.mode = mode;
        return fragment;
    }

    public static IncomeEditBottomSheet newInstance(Income income) {
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
        
        memberRepository = new MemberRepository(requireContext());
        incomeRepository = new IncomeRepository(requireContext());

        setupUI();

        getBinding().btnSave.setOnClickListener(v -> handleSave());
    }

    private void setupUI() {
        switch (mode) {
            case ADD_INCOME:
                getBinding().tvDialogTitle.setText(R.string.title_add_income);
                getBinding().cardSource.setVisibility(View.VISIBLE);
                getBinding().etIncomeSource.setHint(R.string.hint_income_source);
                getBinding().etSalaryAmount.setText("");
                getBinding().btnSave.setText(R.string.label_add_income);
                break;

            case EDIT_INCOME:
                getBinding().tvDialogTitle.setText(R.string.title_edit_income_extra);
                getBinding().cardSource.setVisibility(View.VISIBLE);
                if (incomeToEdit != null) {
                    getBinding().etIncomeSource.setText(incomeToEdit.getSource());
                    getBinding().etSalaryAmount.setText(String.valueOf(incomeToEdit.getAmount()));
                }
                getBinding().btnSave.setText(R.string.label_update);
                break;
        }
    }

    private void handleSave() {
        if (Validator.isEmpty(getBinding().etSalaryAmount)) {
            showToast("Please enter an amount", com.upreyvan.carti.util.ToastHelper.Status.WARNING);
            return;
        }

        if (Validator.isEmpty(getBinding().etIncomeSource)) {
            showToast("Please enter a source", com.upreyvan.carti.util.ToastHelper.Status.WARNING);
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
            showToast("Invalid amount", com.upreyvan.carti.util.ToastHelper.Status.ERROR);
        }
    }

    private void addExtraIncome(String source, double amount) {
        incomeRepository.addIncome(source, amount, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
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
        incomeRepository.updateIncome(incomeToEdit.getId(), source, amount, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
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
            showToast(message, com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
            dismiss();
        });
    }

    private void onActionError(Throwable error) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            setLoading(false);
            showToast("Error: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
        });
    }

    private void setLoading(boolean loading) {
        getBinding().btnSave.setEnabled(!loading);
        getBinding().btnSave.setText(loading ? R.string.label_saving : (mode == Mode.ADD_INCOME ? R.string.label_add_income : R.string.label_save));
    }

    private void showToast(String message, com.upreyvan.carti.util.ToastHelper.Status status) {
        com.upreyvan.carti.util.ToastHelper.show(requireContext(), message, status);
    }
}
