package com.upreyvan.carti.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.DialogQuickLogAmountBinding;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import java.util.Map;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;

public class QuickLogDialog extends BaseBottomSheetFragment<DialogQuickLogAmountBinding> {

    private static final String ARG_ITEM = "arg_item";
    private OnLogListener listener;

    public interface OnLogListener {
        void onLog(QuickLogItem item, double amount);
    }

    public static QuickLogDialog newInstance(QuickLogItem item) {
        QuickLogDialog fragment = new QuickLogDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnLogListener listener) {
        this.listener = listener;
    }

    @Override
    protected DialogQuickLogAmountBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return DialogQuickLogAmountBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        QuickLogItem item = (QuickLogItem) getArguments().getSerializable(ARG_ITEM);
        if (item == null) {
            dismiss();
            return;
        }

        getBinding().tvCategoryName.setText(item.getTitle());

        getBinding().btnLog.setOnClickListener(v -> {
            if (!checkNetwork()) return;

            if (Validator.isEmpty(getBinding().etAmount)) {
                showToast(getString(R.string.msg_enter_amount), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                return;
            }

            try {
                double amount = Double.parseDouble(getBinding().etAmount.getText().toString().trim());
                
                com.upreyvan.carti.util.TransactionHandler.saveTrack(requireContext(), amount, item.getTitle(), "Quick Log", "App", new com.upreyvan.carti.util.TransactionHandler.TransactionCallback() {
                    @Override
                    public void onLoading(boolean isLoading) {
                        if (getBinding() != null) {
                            getBinding().btnLog.setEnabled(!isLoading);
                            getBinding().btnLog.setText(isLoading ? getString(R.string.label_logging) : getString(R.string.btn_log_expense));
                        }
                    }

                    @Override
                    public void onSuccess(Transaction transaction) {
                        if (listener != null) {
                            listener.onLog(item, amount);
                        }
                        dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        showToast(message, com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                    }
                });
            } catch (NumberFormatException e) {
                showToast(getString(R.string.msg_invalid_amount), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    protected void showToast(String message, com.upreyvan.carti.util.ToastHelper.Status status) {
        com.upreyvan.carti.util.ToastHelper.show(requireContext(), message, status);
    }
}
