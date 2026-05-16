package com.upreyvan.carti.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.DialogQuickLogAmountBinding;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import android.view.WindowManager;

public class QuickLogDialog extends BottomSheetDialogFragment {


    private static final String ARG_ITEM = "arg_item";
    private DialogQuickLogAmountBinding binding;
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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            dialog.getWindow().setNavigationBarColor(requireContext().getColor(R.color.white));

            View decorView = dialog.getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decorView.setSystemUiVisibility(flags);
        }

        dialog.getWindow().setDimAmount(0.4f);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogQuickLogAmountBinding.inflate(inflater, container, false);
        return binding.getRoot();
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

        binding.tvCategoryName.setText(item.getTitle());
        binding.btnLog.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.carti_primary_green));

        binding.btnLog.setOnClickListener(v -> {
            String amountStr = binding.etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());
                
                int iconColor = ContextCompat.getColor(requireContext(), item.getIconColor());
                int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);
                
                // Save to cloud
                new ApiHelper(requireContext()).addTransaction(amount, "EXPENSE", item.getTitle(), "Quick Log", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        String id = String.valueOf(result.get("$id"));
                        String familyId = new com.upreyvan.carti.data.local.PreferenceManager(requireContext()).getFamilyId();
                        Transaction transaction = new Transaction(
                                id,
                                familyId,
                                item.getTitle(),
                                time,
                                "₱" + String.format(Locale.getDefault(), "%.2f", amount),
                                item.getIconRes(),
                                bgColor,
                                iconColor,
                                System.currentTimeMillis(),
                                "EXPENSE"
                        );
                        // Save locally first for instant feedback
                        transactionRepository.saveLocally(transaction);
                    }

                    @Override
                    public void onError(Throwable error) {
                        // Optional: Handle cloud save error
                    }
                });

                if (listener != null) {
                    listener.onLog(item, amount);
                }
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
