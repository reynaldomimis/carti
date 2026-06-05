package com.upreyvan.carti.ui.track;

import android.os.Bundle;
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
import com.upreyvan.carti.util.Validator;

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
        getBinding().btnSave.setOnClickListener(v -> handleSave());
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

        if (Validator.isEmpty(getBinding().etAmount) || Validator.isEmpty(getBinding().etDescription)) {
            showToast(R.string.msg_fill_all_fields, UiHelper.Status.WARNING);
            return;
        }

        try {
            double amount = Double.parseDouble(getBinding().etAmount.getText().toString().trim());
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
        getBinding().btnSave.setEnabled(!loading);
        getBinding().btnSave.setText(loading ? R.string.label_saving : R.string.label_update);
    }
}
