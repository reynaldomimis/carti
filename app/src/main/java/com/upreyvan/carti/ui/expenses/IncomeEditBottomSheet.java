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

public class IncomeEditBottomSheet extends BottomSheetDialogFragment {

    public enum Mode {
        ADD_INCOME,
        EDIT_INCOME
    }

    private DialogEditIncomeBinding binding;
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
        binding = DialogEditIncomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        memberRepository = new MemberRepository(requireContext());
        incomeRepository = new IncomeRepository(requireContext());
        SalaryManager manager = SalaryManager.getInstance(requireContext());

        setupUI();

        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void setupUI() {
        binding.btnSave.setBackgroundColor(requireContext().getColor(R.color.carti_primary_green));
        
        switch (mode) {
            case ADD_INCOME:
                binding.tvDialogTitle.setText(R.string.title_add_income);
                binding.cardSource.setVisibility(View.VISIBLE);
                binding.etIncomeSource.setHint(R.string.hint_income_source);
                binding.etSalaryAmount.setText("");
                binding.btnSave.setText(R.string.label_add_income);
                break;

            case EDIT_INCOME:
                binding.tvDialogTitle.setText(R.string.title_edit_income_extra);
                binding.cardSource.setVisibility(View.VISIBLE);
                if (incomeToEdit != null) {
                    binding.etIncomeSource.setText(incomeToEdit.getSource());
                    binding.etSalaryAmount.setText(String.valueOf(incomeToEdit.getAmount()));
                }
                binding.btnSave.setText(R.string.label_update);
                break;
        }
    }

    private void handleSave() {
        String amountStr = binding.etSalaryAmount.getText().toString().trim();
        String source = binding.etIncomeSource.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (source.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a source", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            setLoading(true);

            if (mode == Mode.ADD_INCOME) {
                addExtraIncome(source, amount);
            } else if (mode == Mode.EDIT_INCOME) {
                updateExtraIncome(source, amount);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private void onActionError(Throwable error) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            setLoading(false);
            Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setLoading(boolean loading) {
        binding.btnSave.setEnabled(!loading);
        binding.btnSave.setText(loading ? R.string.label_saving : (mode == Mode.ADD_INCOME ? R.string.label_add_income : R.string.label_save));
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
