package com.upreyvan.carti.ui.goals;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.ToastHelper.Status;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ValueHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UpdateGoalActivity extends BaseGoalActivity {

    public static final String EXTRA_GOAL_ID = "extra_goal_id";
    private String goalId;
    private Transaction currentGoal;
    private TransactionRepository transactionRepository;

    @Override
    protected void initForm() {
        goalId = getIntent().getStringExtra(EXTRA_GOAL_ID);
        if (goalId == null) {
            finish();
            return;
        }

        transactionRepository = TransactionRepository.getInstance(this);
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.update_goal_title);
        getBinding().btnCreateGoal.setText(R.string.btn_update_goal);

        observeGoal();
    }

    private void observeGoal() {
        transactionRepository.getTransactionById(goalId).observe(this, goal -> {
            if (goal != null) {
                currentGoal = goal.getTransaction();
                preFillData();
            }
        });
    }

    private void preFillData() {
        getBinding().etGoalName.setText(ValueHelper.toStr(currentGoal.getTitle()));
        getBinding().etTargetAmount.setText(String.valueOf(currentGoal.getTargetAmount()));
        getBinding().etTargetDate.setText(ValueHelper.toStr(currentGoal.getDueDate()));

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
        double targetAmount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String date = getBinding().etTargetDate.getText().toString().trim();

        showLoading(true, "Updating goal...");

        currentGoal.setTitle(name);
        currentGoal.setTargetAmount(targetAmount);
        currentGoal.setDueDate(date);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transactionRepository != null) transactionRepository.onDestroy();
    }
}