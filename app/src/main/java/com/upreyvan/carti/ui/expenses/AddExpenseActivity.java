package com.upreyvan.carti.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.upreyvan.carti.R;
import android.app.ProgressDialog;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityAddExpenseBinding;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;

import java.util.Map;

import com.upreyvan.carti.util.Validator;

public class AddExpenseActivity extends BaseActivity<ActivityAddExpenseBinding> {

    private ProgressDialog progressDialog;
    private TransactionRepository transactionRepository;

    @Override
    protected ActivityAddExpenseBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddExpenseBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = new TransactionRepository(this);
        setupDynamicPadding();
        setupToolbar();
        setupDropdowns();
        setupClickListeners();
    }

    private void setupDropdowns() {
        List<Category> categories = CategoryManager.getInstance(this).getCategories();
        String[] categoryNames = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            categoryNames[i] = categories.get(i).getName();
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        getBinding().etCategory.setAdapter(categoryAdapter);
        if (categoryNames.length > 0) {
            getBinding().etCategory.setText(categoryNames[0], false);
        }

        String[] sources = {
                "Cash",
                "GCash",
                "Maya",
                "Bank Transfer",
                "Credit Card"
        };

        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, sources);
        getBinding().etSource.setAdapter(sourceAdapter);
        getBinding().etSource.setText(sources[0], false);
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
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_expense_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            if (Validator.isEmpty(getBinding().etAmount) || Validator.isEmpty(getBinding().etCategory) || Validator.isEmpty(getBinding().etSource)) {
                showToast("Please fill all fields", com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                return;
            }

            String amount = getBinding().etAmount.getText().toString();
            String category = getBinding().etCategory.getText().toString();
            String source = getBinding().etSource.getText().toString();
            double amountVal = Double.parseDouble(amount);
            
            showLoading(true);

            new ApiHelper(this).addTransaction(amountVal, "EXPENSE", category, "Paid through " + source, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    String id = String.valueOf(result.get("$id"));
                    saveLocalAndFinish(id, amountVal, category, source);
                }

                @Override
                public void onError(Throwable error) {
                    showLoading(false);
                    showToast("Failed to save: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                }
            });
        });
    }

    private void showLoading(boolean loading) {
        getBinding().btnSave.setEnabled(!loading);
        if (loading) {
            progressDialog = ProgressDialog.show(this, "", "Saving expense...", true);
        } else if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void saveLocalAndFinish(String id, double amountVal, String category, String source) {
        String familyId = new com.upreyvan.carti.data.local.PreferenceManager(this).getFamilyId();
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());
        
        Category selectedCategory = null;
        List<Category> categories = CategoryManager.getInstance(this).getCategories();
        for (Category cat : categories) {
            if (cat.getName().equals(category)) {
                selectedCategory = cat;
                break;
            }
        }

        int iconRes = (selectedCategory != null) ? selectedCategory.getIconRes() : R.drawable.ic_chart;
        int iconColor = (selectedCategory != null) ? getColor(selectedCategory.getIconColor()) : getColor(R.color.icon_others);
        int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);

        Transaction transaction = new Transaction(
                id,
                familyId,
                category,
                "", // description
                time,
                amountVal,
                iconRes,
                bgColor,
                iconColor,
                System.currentTimeMillis(),
                "EXPENSE"
        );
        transactionRepository.saveLocally(transaction);

        showLoading(false);
        showToast("Expense Saved!", com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
        
        Intent intent = new Intent(this, com.upreyvan.carti.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
