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
import com.upreyvan.carti.databinding.DialogEditSalaryBinding;

public class SalaryEditBottomSheet extends BottomSheetDialogFragment {

    private DialogEditSalaryBinding binding;
    private OnSalaryUpdatedListener listener;

    public interface OnSalaryUpdatedListener {
        void onSalaryUpdated();
    }

    public static SalaryEditBottomSheet newInstance() {
        return new SalaryEditBottomSheet();
    }

    public void setListener(OnSalaryUpdatedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
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
        binding = DialogEditSalaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        binding.etSalaryAmount.setText(String.valueOf(manager.getSalaryAmount()));
        binding.btnSave.setBackgroundColor(requireContext().getColor(R.color.carti_primary_green));

        binding.btnSave.setOnClickListener(v -> {
            String amountStr = binding.etSalaryAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                float amount = Float.parseFloat(amountStr);
                manager.setSalaryAmount(amount);
                if (listener != null) {
                    listener.onSalaryUpdated();
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
