package com.upreyvan.carti.ui.goals;

import com.upreyvan.carti.R;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionType;
import com.upreyvan.carti.utils.UiHelper.Status;
import com.upreyvan.carti.utils.Validator;
import java.util.ArrayList;
import java.util.Map;

public class AddGoalActivity extends BaseGoalActivity {

    @Override
    protected void initForm() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_goal_title);
        getBinding().btnCreateGoal.setText(R.string.btn_create_goal);
        preSelectAllMembers();
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
        goal.setTitle(name);
        goal.setTargetAmount(targetAmount);
        goal.setTargetDate(date);
        goal.setCategory("Goal");
        goal.setMembers(new ArrayList<>(selectedMemberIds));
        goal.setUserId(PreferenceManager.getInstance(this).getUserId());

        transactionRepository.createItem(TransactionType.GOAL, goal, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                showLoading(false);
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

