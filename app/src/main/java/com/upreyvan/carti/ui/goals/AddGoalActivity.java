package com.upreyvan.carti.ui.goals;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;

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
            showToast(getString(R.string.msg_fill_all_fields), ToastHelper.Status.WARNING);
            return;
        }

        String name = getBinding().etGoalName.getText().toString().trim();
        double targetAmount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String date = getBinding().etTargetDate.getText().toString().trim();
        String contributorIds = String.join(",", selectedMemberIds);

        showLoading(true, getString(R.string.msg_saving_goal));

        new ApiHelper(this).addGoal(name, targetAmount, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                String id = String.valueOf(result.get("$id"));
                String familyId = new PreferenceManager(AddGoalActivity.this).getFamilyId();

                goalRepository.saveLocally(
                        new Goal(id, familyId, name, "", 0, targetAmount, date, R.drawable.ic_image,
                                androidx.core.content.ContextCompat.getColor(AddGoalActivity.this, R.color.goal_card_1),
                                contributorIds)
                );

                showLoading(false);
                showToast(getString(R.string.msg_goal_saved_success), ToastHelper.Status.SUCCESS);
                finish();
            }

            @Override
            public void onError(Throwable error) {
                showLoading(false);
                showToast(getString(R.string.err_generic, error.getMessage()), ToastHelper.Status.ERROR);
            }
        });
    }
}
