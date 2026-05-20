package com.upreyvan.carti.ui.goals;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.ToastHelper.Status;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ValueHelper;
import java.util.ArrayList;
import java.util.Map;

public class AddGoalActivity extends BaseGoalActivity {

    @Override
    protected void initForm() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_goal_title);
        getBinding().btnCreateGoal.setText(R.string.btn_create_goal);
        memberAdapter.submitList(new ArrayList<>());
    }

    @Override
    protected void onSaveClicked() {
        if (!checkNetwork()) return;

        if (Validator.isEmpty(getBinding().etGoalName) || Validator.isEmpty(getBinding().etTargetAmount)) {
            showToast(getString(R.string.msg_fill_all_fields), Status.WARNING);
            return;
        }

        String name = getBinding().etGoalName.getText().toString().trim();
        double targetAmount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String date = getBinding().etTargetDate.getText().toString().trim();

        showLoading(true, getString(R.string.msg_saving_goal));

        Transaction goal = new Transaction();
        goal.setType("GOAL");
        goal.setTitle(name);
        goal.setTargetAmount(targetAmount);
        goal.setDueDate(date);
        goal.setCategory("Goal");
        goal.setMembers(new ArrayList<>(selectedMemberIds));
        goal.setUserId(new PreferenceManager(this).getUserId());

        transactionRepository.addTransaction(goal, new AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                showLoading(false);
                showToast(getString(R.string.msg_goal_saved_success), Status.SUCCESS);
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