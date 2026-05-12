package com.upreyvan.carti.ui.goals;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityAddGoalBinding;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddGoalActivity extends BaseActivity<ActivityAddGoalBinding> {

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
            String amount = getBinding().etTargetAmount.getText().toString();

            if (name.isEmpty() || amount.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Goal Saved Successfully!", Toast.LENGTH_SHORT).show();
            
            // Return to MainActivity and show Home tab
            Intent intent = new Intent(this, com.upreyvan.carti.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("show_home", true);
            startActivity(intent);
            finish();
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
}
