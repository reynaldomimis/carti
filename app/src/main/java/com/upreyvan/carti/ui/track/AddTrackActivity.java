package com.upreyvan.carti.ui.track;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.databinding.ActivityAddTrackBinding;
import com.upreyvan.carti.databinding.LayoutExpenseFormBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.StringHelper;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.TransactionHandler;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;

import java.util.List;

public class AddTrackActivity extends BaseActivity<ActivityAddTrackBinding> {

    @Override
    protected ActivityAddTrackBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddTrackBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupDropdowns();
        setupClickListeners();
        getBinding().layoutForm.cvBalanceInfo.setVisibility(View.GONE);
    }

    private void setupDropdowns() {
        List<Category> categories = CategoryManager.getInstance(this).getCategories();
        String[] categoryNames = categories.stream()
                .map(Category::getName)
                .toArray(String[]::new);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        getBinding().actvCategory.setAdapter(categoryAdapter);
        if (categoryNames.length > 0) {
            String firstCategory = categoryNames[0];
            getBinding().actvCategory.setText(firstCategory, false);
            updateBalanceInfo(firstCategory);
        }

        getBinding().actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            updateBalanceInfo(selected);
        });

        String[] sources = {"Cash", "GCash", "Maya", "Bank Transfer", "Credit Card"};
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, sources);
        getBinding().layoutForm.actvSource.setAdapter(sourceAdapter);
        getBinding().layoutForm.actvSource.setText(sources[0], false);
    }

    private void updateBalanceInfo(String categoryName) {
        getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE);
        com.upreyvan.carti.util.BudgetAllocationHelper.getRemainingBalance(this, categoryName, balance -> 
            getBinding().layoutForm.tvAllocatedBalance.setText(StringHelper.formatCurrency(balance))
        );
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().btnSave, 1f, 0);
        getBinding().scrollView.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.scroll_bottom_padding));
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_expense_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            if (!checkNetwork()) return;

            if (Validator.isEmpty(getBinding().layoutForm.etAmount) || 
                Validator.isEmpty(getBinding().actvCategory) || 
                Validator.isEmpty(getBinding().layoutForm.etDescription)) {
                showToast(getString(R.string.msg_fill_all_fields), ToastHelper.Status.WARNING);
                return;
            }

            Editable amountText = getBinding().layoutForm.etAmount.getText();
            if (amountText == null) return;

            double amountVal = StringHelper.parseDouble(amountText.toString());
            String category = getBinding().actvCategory.getText().toString();
            String description = getBinding().layoutForm.etDescription.getText().toString();
            String source = getBinding().layoutForm.actvSource.getText().toString();

            TransactionHandler.saveTrack(this, amountVal, category, description, source, new TransactionHandler.TransactionCallback() {
                @Override
                public void onLoading(boolean isLoading) {
                    showLoading(isLoading, getString(R.string.msg_saving_expense));
                }

                @Override
                public void onSuccess(Transaction transaction) {
                    Intent intent = new Intent(AddTrackActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String message) {
                    showToast(getString(R.string.err_failed_save, message), ToastHelper.Status.ERROR);
                }
            });
        });
    }
}
