package com.upreyvan.carti.ui.debt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.app.ProgressDialog;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.ActivityAddDebtBinding;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class AddDebtActivity extends BaseActivity<ActivityAddDebtBinding> {

    private ProgressDialog progressDialog;

    @Override
    protected ActivityAddDebtBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddDebtBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupDefaults();
        setupClickListeners();
    }

    private void setupDefaults() {
        // Repeat Options Dropdown
        String[] options = {
                getString(R.string.opt_no),
                getString(R.string.opt_yes),
                "Every Week",
                "Every Month"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, options);
        getBinding().etRepeat.setAdapter(adapter);
        getBinding().etRepeat.setText(options[0], false); // Default to No

        // Default Date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        getBinding().tvDueDate.setText(sdf.format(calendar.getTime()));
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
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_debt_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        getBinding().btnDueDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Due Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(selection);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                getBinding().tvDueDate.setText(sdf.format(calendar.getTime()));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().etBorrowerName.getText().toString();
            String amountStr = getBinding().etAmount.getText().toString();

            if (name.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill name and amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            showLoading(true);

            new ApiHelper(this).addDebt(name, amount, "OWE", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    String id = String.valueOf(result.get("$id"));
                    com.upreyvan.carti.data.local.DebtManager.getInstance().addDebt(
                            new com.upreyvan.carti.model.Debt(id, name, "", "Upcoming", amount, false, R.drawable.ic_person, "")
                    );

                    showLoading(false);
                    Toast.makeText(AddDebtActivity.this, "Debt Saved!", Toast.LENGTH_SHORT).show();
                    
                    // Return to MainActivity and clear the stack (removes AddOptionsActivity)
                    Intent intent = new Intent(AddDebtActivity.this, com.upreyvan.carti.MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(Throwable error) {
                    showLoading(false);
                    Toast.makeText(AddDebtActivity.this, "Error saving debt: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showLoading(boolean loading) {
        getBinding().btnSave.setEnabled(!loading);
        if (loading) {
            progressDialog = ProgressDialog.show(this, "", "Saving debt...", true);
        } else if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
