package com.upreyvan.carti.ui.allocate;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.BottomSheetAddBudgetBinding;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class AddBudgetBottomSheet extends BaseBottomSheetFragment<BottomSheetAddBudgetBinding> {

    private BudgetCategoryItem editingItem;

    public static AddBudgetBottomSheet newInstance() {
        return new AddBudgetBottomSheet();
    }

    public static AddBudgetBottomSheet newInstance(BudgetCategoryItem item) {
        AddBudgetBottomSheet fragment = new AddBudgetBottomSheet();
        fragment.editingItem = item;
        return fragment;
    }

    @Override
    protected BottomSheetAddBudgetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetAddBudgetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        setupCategoryDropdown();
        
        getBinding().btnClose.setOnClickListener(v1 -> dismiss());
        getBinding().btnCancel.setOnClickListener(v1 -> dismiss());
        getBinding().btnSave.setOnClickListener(v1 -> saveBudget());

        if (editingItem != null) {
            getBinding().tvTitle.setText(R.string.btn_edit);
            getBinding().etCategory.setText(editingItem.getCategoryName());
            getBinding().etCategory.setEnabled(false); // Usually don't want to change category name here
            getBinding().etLimit.setText(String.valueOf(editingItem.getAmount()));
            getBinding().switchRecurring.setChecked(editingItem.isRecurring());
        }
    }

    private void setupCategoryDropdown() {
        List<BudgetCategoryItem> categories = BudgetManager.getInstance(requireContext()).getBudgetPlan();
        List<String> names = new ArrayList<>();
        for (BudgetCategoryItem item : categories) names.add(item.getCategoryName());
        
        // Sort to keep "Others" always at the end
        names.sort((a, b) -> {
            if (a.equalsIgnoreCase("Others")) return 1;
            if (b.equalsIgnoreCase("Others")) return -1;
            return a.compareToIgnoreCase(b);
        });
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
        getBinding().etCategory.setAdapter(adapter);
    }

    private void saveBudget() {
        Editable categoryText = getBinding().etCategory.getText();
        Editable amountText = getBinding().etLimit.getText();

        if (categoryText == null || amountText == null) return;

        String category = categoryText.toString().trim();
        String amountStr = amountText.toString().trim();
        boolean isRecurring = getBinding().switchRecurring.isChecked();
        
        if (category.isEmpty() || amountStr.isEmpty()) {
            showToast(R.string.msg_fill_all_fields, UiHelper.Status.ERROR);
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
            t.setAllocationMonth(Utils.getCurrentTimestamp());
            
            TransactionRepository.getInstance(requireContext()).addTransaction(t, null);
            dismiss();
        } catch (NumberFormatException e) {
            showToast(R.string.msg_invalid_amount, UiHelper.Status.ERROR);
        }
    }
}
