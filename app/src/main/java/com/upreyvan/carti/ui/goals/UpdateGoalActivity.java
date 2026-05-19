package com.upreyvan.carti.ui.goals;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;

import java.util.List;
import java.util.Map;

public class UpdateGoalActivity extends BaseGoalActivity {

    public static final String EXTRA_GOAL_ID = "extra_goal_id";
    private String goalId;
    private Goal currentGoal;

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
        goalRepository.getGoalById(goalId).observe(this, goal -> {
            if (goal != null) {
                currentGoal = goal;
                preFillData();
            }
        });
    }

    private void preFillData() {
        getBinding().etGoalName.setText(currentGoal.getTitle());
        getBinding().etTargetAmount.setText(String.valueOf(currentGoal.getTargetAmount()));
        getBinding().etTargetDate.setText(currentGoal.getTargetDate());

        String contributorIdsStr = currentGoal.getContributorIds();
        if (contributorIdsStr != null && !contributorIdsStr.isEmpty()) {
            List<String> ids = java.util.Arrays.asList(contributorIdsStr.split(","));
            selectedMemberIds.addAll(ids);
            memberRepository.getMembersByIds(ids).observe(this, members -> {
                if (members != null) {
                    memberAdapter.submitList(new java.util.ArrayList<>(members));
                }
            });
        }
    }

    @Override
    protected void onSaveClicked() {
        if (!checkNetwork()) return;
        if (currentGoal == null) return;

        if (Validator.isEmpty(getBinding().etGoalName) || Validator.isEmpty(getBinding().etTargetAmount)) {
            showToast(getString(R.string.msg_fill_all_fields), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
            return;
        }

        String name = getBinding().etGoalName.getText().toString().trim();
        double targetAmount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String date = getBinding().etTargetDate.getText().toString().trim();
        String contributorIds = String.join(",", selectedMemberIds);

        showLoading(true, "Updating goal...");

        currentGoal.setTitle(name);
        currentGoal.setTargetAmount(targetAmount);
        currentGoal.setTargetDate(date);
        currentGoal.setContributorIds(contributorIds);

        goalRepository.updateGoal(currentGoal, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                showLoading(false);
                showToast("Goal updated successfully", com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
                finish();
            }

            @Override
            public void onError(Throwable error) {
                showLoading(false);
                showToast(getString(R.string.err_generic, error.getMessage()), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }
}
