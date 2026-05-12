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
import com.upreyvan.carti.databinding.DialogEditPaydayBinding;

public class PaydayEditBottomSheet extends BottomSheetDialogFragment {

    private DialogEditPaydayBinding binding;
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
        binding = DialogEditPaydayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        binding.etPayday1.setText(String.valueOf(manager.getFirstPayday()));
        binding.etPayday2.setText(String.valueOf(manager.getSecondPayday()));
        binding.btnSave.setBackgroundColor(requireContext().getColor(R.color.carti_primary_green));

        binding.btnSave.setOnClickListener(v -> {
            String p1Str = binding.etPayday1.getText().toString().trim();
            String p2Str = binding.etPayday2.getText().toString().trim();
            
            if (p1Str.isEmpty() || p2Str.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both paydays", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int p1 = Integer.parseInt(p1Str);
                int p2 = Integer.parseInt(p2Str);
                
                if (p1 < 1 || p1 > 31 || p2 < 1 || p2 > 31) {
                    Toast.makeText(requireContext(), "Days must be between 1 and 31", Toast.LENGTH_SHORT).show();
                    return;
                }

                manager.setFirstPayday(p1);
                manager.setSecondPayday(p2);
                if (listener != null) {
                    listener.onPaydayUpdated();
                }
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid day format", Toast.LENGTH_SHORT).show();
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
