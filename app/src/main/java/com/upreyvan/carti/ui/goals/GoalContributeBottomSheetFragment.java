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
        
        // Show Target and Remaining prominent at the top
        double currentSum = goal.getAmount(); // This is the master sum from DB
        double remaining = Math.max(0, goal.getTargetAmount() - currentSum);
        
        getBinding().tvSubtitle.setText(String.format("Target: %s • Remaining: %s", 
                Utils.formatCurrency(goal.getTargetAmount()),
                Utils.formatCurrency(remaining)));
        
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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                getBinding().tilAmount.setError(null);
                validate(); 
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        getBinding().btnContribute.setOnClickListener(v -> {
            if (!checkNetwork()) return;
            double amount = StringHelper.parseDouble(getBinding().etAmount.getText().toString());
            
            double currentSum = goal.getAmount();
            double remaining = goal.getTargetAmount() - currentSum;
            
            if (amount > (remaining + 0.01)) {
                getBinding().tilAmount.setError("Amount exceeds remaining target of " + Utils.formatCurrency(remaining));
                return;
            }
            
            saveContribution(amount);
        });
    }

    private void validate() {
        String amountStr = getBinding().etAmount.getText().toString().trim();
        double amount = StringHelper.parseDouble(amountStr);
        boolean isAmountEntered = !amountStr.isEmpty();
        
        double currentSum = goal.getAmount();
        double remaining = goal.getTargetAmount() - currentSum;
        
        if (isAmountEntered && amount > (remaining + 0.01)) {
            getBinding().tilAmount.setError("Exceeds target limit");
            getBinding().btnContribute.setEnabled(false);
        } else {
            getBinding().tilAmount.setError(null);
            getBinding().btnContribute.setEnabled(isAmountEntered && amount > 0);
        }
    }

    private void saveContribution(double amount) {
        showLoading(true, "Adding funds to goal...");

        com.upreyvan.carti.models.Transaction t = new com.upreyvan.carti.models.Transaction();
        t.setAmount(amount);
        t.setType("GOAL"); 
        t.setCategory(goal.getTitle());
        t.setTitle("Contribution: " + goal.getTitle());
        
        // UNIQUE LINKING: Use the parent's tag (G-XXXX) so all transactions share the same group ID
        String groupTag = (goal.getAllocatedTo() != null && !goal.getAllocatedTo().isEmpty()) ? goal.getAllocatedTo() : goal.getId();
        t.setAllocatedTo(groupTag);

        // Inherit metadata from parent goal
        t.setTargetDate(goal.getTargetDate());
        t.setMembers(goal.getMembers());
        
        // UX CHANGE: Manual completion flow. Status remains ACTIVE even if target reached.
        t.setStatus("ACTIVE");

        t.setAllocationMonth(com.upreyvan.carti.utils.Utils.formatMonthQuery(java.util.Calendar.getInstance()));
        
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
