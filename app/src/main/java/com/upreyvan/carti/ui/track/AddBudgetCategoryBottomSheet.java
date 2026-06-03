package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddBudgetCategoryBinding;
import com.upreyvan.carti.util.ToastHelper;

public class AddBudgetCategoryBottomSheet extends BaseBottomSheetFragment<LayoutBottomSheetAddBudgetCategoryBinding> {

    private OnCategoryAddedListener listener;

    public interface OnCategoryAddedListener {
        void onCategoryAdded(String name, double amount);
    }

    public void setListener(OnCategoryAddedListener listener) {
        this.listener = listener;
    }

    @Override
    protected LayoutBottomSheetAddBudgetCategoryBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutBottomSheetAddBudgetCategoryBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().etCategoryName.getText().toString().trim();
            String amountStr = getBinding().etCategoryAmount.getText().toString().trim();

            if (name.isEmpty() || amountStr.isEmpty()) {
                ToastHelper.show(requireContext(), R.string.msg_fill_all_fields, ToastHelper.Status.ERROR);
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (listener != null) {
                    listener.onCategoryAdded(name, amount);
                }
                dismiss();
            } catch (NumberFormatException e) {
                ToastHelper.show(requireContext(), R.string.msg_invalid_amount, ToastHelper.Status.ERROR);
            }
        });
    }
}
