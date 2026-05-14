package com.upreyvan.carti.ui.goals;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import android.app.ProgressDialog;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.ActivityAddGoalBinding;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class AddGoalActivity extends BaseActivity<ActivityAddGoalBinding> {

    private ProgressDialog progressDialog;

    @Override
    protected ActivityAddGoalBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddGoalBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupDefaults();
        setupListeners();
    }

    private void setupDefaults() {
        // Default Date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1); // Default to next month for goal
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        getBinding().tvSelectedDate.setText(sdf.format(calendar.getTime()));
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnSave,
                1f,
                0
        );
        getBinding().scrollView.setPadding(0, 0, 0, Utils.dpToPx(this, 120));
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_goal_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().etGoalName.getText().toString();
            String amountStr = getBinding().etTargetAmount.getText().toString();

            if (name.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double targetAmount = Double.parseDouble(amountStr);
            showLoading(true);

            new ApiHelper(this).addGoal(name, targetAmount, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    showLoading(false);
                    Toast.makeText(AddGoalActivity.this, "Goal Saved Successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Return to MainActivity and show Home tab
                    Intent intent = new Intent(AddGoalActivity.this, com.upreyvan.carti.MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("show_home", true);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(Throwable error) {
                    showLoading(false);
                    Toast.makeText(AddGoalActivity.this, "Error saving goal: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        getBinding().btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Target Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(selection);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                getBinding().tvSelectedDate.setText(sdf.format(calendar.getTime()));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }

    private void showLoading(boolean loading) {
        getBinding().btnSave.setEnabled(!loading);
        if (loading) {
            progressDialog = ProgressDialog.show(this, "", "Saving goal...", true);
        } else if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
