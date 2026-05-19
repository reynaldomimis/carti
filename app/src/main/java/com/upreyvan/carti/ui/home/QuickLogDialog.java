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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import com.upreyvan.carti.util.Validator;

public class QuickLogDialog extends BaseBottomSheetFragment<DialogQuickLogAmountBinding> {

    private static final String ARG_ITEM = "arg_item";
    private OnLogListener listener;
    private TransactionRepository transactionRepository;

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
        transactionRepository = new TransactionRepository(requireContext());
        
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
                String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());
                
                int iconColor = ContextCompat.getColor(requireContext(), item.getIconColor());
                int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);
                
                new ApiHelper(requireContext()).addTransaction(amount, "EXPENSE", item.getTitle(), "Quick Log", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        String id = String.valueOf(result.get("$id"));
                        com.upreyvan.carti.data.local.PreferenceManager pref = new com.upreyvan.carti.data.local.PreferenceManager(requireContext());
                        String familyId = pref.getFamilyId();
                        String userId = pref.getUserId();
                        Transaction transaction = new Transaction(
                                id,
                                familyId,
                                userId,
                                item.getTitle(),
                                "Quick Log", // description
                                time,
                                amount,
                                item.getIconRes(),
                                bgColor,
                                iconColor,
                                System.currentTimeMillis(),
                                "EXPENSE"
                        );
                        transactionRepository.saveLocally(transaction);
                    }

                    @Override
                    public void onError(Throwable error) {
                        showToast(getString(R.string.err_generic, error.getMessage()), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                    }
                });

                if (listener != null) {
                    listener.onLog(item, amount);
                }
                dismiss();
            } catch (NumberFormatException e) {
                showToast(getString(R.string.msg_invalid_amount), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionRepository != null) {
            transactionRepository.onDestroy();
        }
    }

    private void showToast(String message, com.upreyvan.carti.util.ToastHelper.Status status) {
        com.upreyvan.carti.util.ToastHelper.show(requireContext(), message, status);
    }
}
