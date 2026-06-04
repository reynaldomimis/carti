package com.upreyvan.carti.ui.goals;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.ToastHelper.Status;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ValueHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateGoalActivity extends BaseGoalActivity {

    public static final String EXTRA_GOAL_ID = "extra_goal_id";
    private String goalId;
    private Transaction currentGoal;
    private boolean isDataPrefilled = false;

    @Override
    protected void initForm() {
        goalId = getIntent().getStringExtra(EXTRA_GOAL_ID);
        if (goalId == null) {
            finish();
            return;
        }

        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.update_goal_title);
        getBinding().btnCreateGoal.setText(R.string.btn_update_goal);

        observeGoal();
    }

    private void observeGoal() {
        transactionRepository.getTransactionById(goalId).observe(this, goal -> {
            if (goal != null && !isDataPrefilled) {
                currentGoal = goal.getTransaction();
                preFillData();
                isDataPrefilled = true;
            }
        });
    }

    private void preFillData() {
        if (currentGoal == null) return;
        
        getBinding().etGoalName.setText(ValueHelper.toStr(currentGoal.getTitle()));
        getBinding().etTargetAmount.setText(String.valueOf(currentGoal.getTargetAmount()));
        getBinding().etTargetDate.setText(ValueHelper.toStr(currentGoal.getTargetDate()));

        List<String> ids = currentGoal.getMembers();
        if (ids != null && !ids.isEmpty()) {
            selectedMemberIds.addAll(ids);
            memberRepository.getMembersByIds(ids).observe(this, memberList -> {
                if (memberList != null) {
                    memberAdapter.submitList(new ArrayList<>(memberList));
                }
            });
        }
    }

    @Override
    protected void onSaveClicked() {
        if (!checkNetwork()) return;
        if (currentGoal == null) return;

        if (Validator.isEmpty(getBinding().etGoalName) || Validator.isEmpty(getBinding().etTargetAmount)) {
            showToast(getString(R.string.msg_fill_all_fields), Status.WARNING);
            return;
        }

        String name = getBinding().etGoalName.getText().toString().trim();
        String amountStr = getBinding().etTargetAmount.getText().toString().trim();
        String date = getBinding().etTargetDate.getText().toString().trim();

        double targetAmount;
        try {
            targetAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            showToast("Invalid amount", Status.ERROR);
            return;
        }

        showLoading(true, "Updating goal...");

        currentGoal.setTitle(name);
        currentGoal.setTargetAmount(targetAmount);
        currentGoal.setTargetDate(date);
        currentGoal.setMembers(new ArrayList<>(selectedMemberIds));

        transactionRepository.updateTransaction(currentGoal, new AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                showLoading(false);
                showToast("Goal updated successfully", Status.SUCCESS);
                finish();
            }

            @Override
            public void onError(Throwable error) {
                showLoading(false);
                showToast(getString(R.string.err_generic, error.getMessage()), Status.ERROR);
            }
        });
    }
}
