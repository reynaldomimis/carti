package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.databinding.DialogEditPaydayBinding;

import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.util.Validator;

public class PaydayEditBottomSheet extends BaseBottomSheetFragment<DialogEditPaydayBinding> {

    private OnPaydayUpdatedListener listener;

    public interface OnPaydayUpdatedListener {
        void onPaydayUpdated();
    }

    public static PaydayEditBottomSheet newInstance() {
        return new PaydayEditBottomSheet();
    }

    public void setListener(OnPaydayUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    protected DialogEditPaydayBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return DialogEditPaydayBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        getBinding().etPayday1.setText(String.valueOf(manager.getFirstPayday()));
        getBinding().etPayday2.setText(String.valueOf(manager.getSecondPayday()));
        getBinding().cbMonthly.setChecked(manager.isMonthly());
        updateInputStates(manager.isMonthly());

        getBinding().cbMonthly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateInputStates(isChecked);
        });

        getBinding().btnSave.setOnClickListener(v -> {
            boolean isMonthly = getBinding().cbMonthly.isChecked();
            manager.setIsMonthly(isMonthly);

            if (Validator.isEmpty(getBinding().etPayday1)) {
                showToast(getString(R.string.msg_enter_payday), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                return;
            }

            try {
                int p1 = Integer.parseInt(getBinding().etPayday1.getText().toString().trim());
                if (p1 < 1 || p1 > 31) {
                    showToast(getString(R.string.msg_day_range), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                    return;
                }

                if (!isMonthly) {
                    if (Validator.isEmpty(getBinding().etPayday2)) {
                        showToast(getString(R.string.msg_enter_payday_second), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                        return;
                    }
                    int p2 = Integer.parseInt(getBinding().etPayday2.getText().toString().trim());
                    if (p2 < 1 || p2 > 31) {
                        showToast(getString(R.string.msg_day_range), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                        return;
                    }
                    manager.setFirstPayday(p1);
                    manager.setSecondPayday(p2);
                } else {
                    manager.setFirstPayday(p1);
                }
            } catch (NumberFormatException e) {
                showToast(getString(R.string.msg_invalid_day_format), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                return;
            }

            if (listener != null) {
                listener.onPaydayUpdated();
            }
            dismiss();
        });
    }

    private void updateInputStates(boolean isMonthly) {
        getBinding().etPayday1.setEnabled(true);
        getBinding().etPayday2.setEnabled(!isMonthly);
        
        View card2 = (View) getBinding().etPayday2.getParent();
        card2.setAlpha(isMonthly ? 0.3f : 1.0f);
        
        if (isMonthly) {
            getBinding().etPayday1.setHint("Day");
        } else {
            getBinding().etPayday1.setHint("Day 1");
        }
    }

    protected void showToast(String message, com.upreyvan.carti.util.ToastHelper.Status status) {
        com.upreyvan.carti.util.ToastHelper.show(requireContext(), message, status);
    }
}
