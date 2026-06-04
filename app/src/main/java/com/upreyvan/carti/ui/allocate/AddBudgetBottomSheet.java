package com.upreyvan.carti.ui.allocate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.BottomSheetAddBudgetBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class AddBudgetBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetAddBudgetBinding b;

    public static AddBudgetBottomSheet newInstance() {
        return new AddBudgetBottomSheet();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = BottomSheetAddBudgetBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        setupCategoryDropdown();
        
        b.btnClose.setOnClickListener(v1 -> dismiss());
        b.btnCancel.setOnClickListener(v1 -> dismiss());
        b.btnSave.setOnClickListener(v1 -> saveBudget());
    }

    private void setupCategoryDropdown() {
        List<BudgetCategoryItem> categories = BudgetManager.getInstance(requireContext()).getBudgetPlan();
        List<String> names = new ArrayList<>();
        for (BudgetCategoryItem item : categories) names.add(item.getCategoryName());
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
        b.etCategory.setAdapter(adapter);
    }

    private void saveBudget() {
        if (b.etCategory.getText() == null || b.etLimit.getText() == null) return;

        String category = b.etCategory.getText().toString().trim();
        String amountStr = b.etLimit.getText().toString().trim();
        boolean isRecurring = b.switchRecurring.isChecked();
        
        if (category.isEmpty() || amountStr.isEmpty()) {
            ToastHelper.show(requireContext(), R.string.msg_fill_all_fields, ToastHelper.Status.ERROR);
            return;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            BudgetManager.getInstance(requireContext()).updateOrAddCategory(category, amount, null, isRecurring);
            
            Transaction t = new Transaction();
            t.setAmount(amount);
            t.setType("ALLOCATION");
            t.setCategory(category);
            t.setTitle(category);
            t.setAllocatedTo(category);
            t.setAllocationMonth(com.upreyvan.carti.util.Utils.getCurrentTimestamp());
            
            TransactionRepository.getInstance(requireContext()).addTransaction(t, null);
            dismiss();
        } catch (NumberFormatException e) {
            ToastHelper.show(requireContext(), R.string.msg_invalid_amount, ToastHelper.Status.ERROR);
        }
    }
}
