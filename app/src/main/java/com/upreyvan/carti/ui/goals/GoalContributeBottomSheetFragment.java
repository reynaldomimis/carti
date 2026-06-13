package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.BottomSheetContributeBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.utils.AmountTextWatcher;
import com.upreyvan.carti.utils.StringHelper;
import com.upreyvan.carti.utils.UiHelper;
import com.upreyvan.carti.utils.Utils;

public class GoalContributeBottomSheetFragment extends BaseBottomSheetFragment<BottomSheetContributeBinding> {

    private Transaction goal;

    public static GoalContributeBottomSheetFragment newInstance(Transaction goal) {
        GoalContributeBottomSheetFragment fragment = new GoalContributeBottomSheetFragment();
        fragment.goal = goal;
        return fragment;
    }

    @Override
    protected BottomSheetContributeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetContributeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupListeners();
    }

    private void setupUI() {
        if (goal == null) return;
        getBinding().tvTitle.setText("Contribute to " + goal.getTitle());
        getBinding().labelCategory.setVisibility(View.GONE);
        getBinding().layoutCategory.setVisibility(View.GONE);
        getBinding().labelSubCategory.setVisibility(View.GONE);
        getBinding().layoutSubCategory.setVisibility(View.GONE);
        getBinding().btnContribute.setText("Add Funds");
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());

        getBinding().etAmount.addTextChangedListener(new AmountTextWatcher(getBinding().etAmount));
        getBinding().etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validate(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        getBinding().btnContribute.setOnClickListener(v -> {
            if (!checkNetwork()) return;
            double amount = StringHelper.parseDouble(getBinding().etAmount.getText().toString());
            saveContribution(amount);
        });
    }

    private void validate() {
        String amountStr = getBinding().etAmount.getText().toString().trim();
        double amount = StringHelper.parseDouble(amountStr);
        boolean isAmountEntered = !amountStr.isEmpty();
        boolean isValidAmount = amount > 0;
        getBinding().btnContribute.setEnabled(isAmountEntered && isValidAmount);
    }

    private void saveContribution(double amount) {
        showLoading(true, "Adding funds to goal...");

        com.upreyvan.carti.models.Transaction t = new com.upreyvan.carti.models.Transaction();
        t.setAmount(amount);
        t.setType("GOAL"); 
        t.setCategory("allocated"); 
        t.setTitle("Contribution: " + goal.getTitle());
        t.setNote("Manual contribution to goal: " + goal.getTitle());
        t.setAllocatedTo(goal.getId());
        t.setAllocationMonth(com.upreyvan.carti.utils.Utils.formatMonthQuery(java.util.Calendar.getInstance()));
        t.setMembers(goal.getMembers());
        
        com.upreyvan.carti.repository.TransactionRepository.getInstance(requireContext())
            .createItem("GOAL", t,
                new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<java.util.Map<String, Object>>() {
                    @Override
                    public void onSuccess(java.util.Map<String, Object> result) {
                        showLoading(false);
                        showToast("Funds added successfully!", UiHelper.Status.SUCCESS);
                        dismiss();
                    }
                    @Override
                    public void onError(Throwable error) {
                        showLoading(false);
                        showToast("Failed to save: " + error.getMessage(), UiHelper.Status.ERROR);
                    }
                });
    }
}
