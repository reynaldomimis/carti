package com.upreyvan.carti.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.core.graphics.ColorUtils;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import com.upreyvan.carti.R;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityAddExpenseBinding;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.ToastHelper.Status;
import com.upreyvan.carti.util.ValueHelper;
import com.upreyvan.carti.util.FormatUtils;

public class AddExpenseActivity extends BaseActivity<ActivityAddExpenseBinding> {

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
        String[] categoryNames = categories.stream()
                .map(Category::getName)
                .toArray(String[]::new);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        getBinding().etCategory.setAdapter(categoryAdapter);
        if (categoryNames.length > 0) {
            getBinding().etCategory.setText(categoryNames[0], false);
        }

        String[] sources = {"Cash", "GCash", "Maya", "Bank Transfer", "Credit Card"};
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, sources);
        getBinding().etSource.setAdapter(sourceAdapter);
        getBinding().etSource.setText(sources[0], false);
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

            if (Validator.isEmpty(getBinding().etAmount) || Validator.isEmpty(getBinding().etCategory) || Validator.isEmpty(getBinding().etSource)) {
                showToast(getString(R.string.msg_fill_all_fields), Status.WARNING);
                return;
            }

            double amountVal = Utils.getDouble(getBinding().etAmount.getText().toString());
            String category = getBinding().etCategory.getText().toString();
            String source = getBinding().etSource.getText().toString();
            
            showLoading(true, getString(R.string.msg_saving_expense));

            new ApiHelper(this).addTransaction(amountVal, "EXPENSE", category, "Paid through " + source, new AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    saveLocalAndFinish(ValueHelper.toStr(result.get("$id")), amountVal, category, source);
                }

                @Override
                public void onError(Throwable error) {
                    showLoading(false);
                    showToast(getString(R.string.err_failed_save, error.getMessage()), Status.ERROR);
                }
            });
        });
    }

    private void saveLocalAndFinish(String id, double amountVal, String category, String source) {
        PreferenceManager pref = new PreferenceManager(this);
        Category selectedCategory = CategoryManager.getInstance(this).getCategories().stream()
                .filter(cat -> cat.getName().equals(category))
                .findFirst()
                .orElse(null);

        int iconRes = (selectedCategory != null) ? selectedCategory.getIconRes() : R.drawable.ic_chart;
        int iconColor = (selectedCategory != null) ? getColor(selectedCategory.getIconColor()) : getColor(R.color.icon_others);
        int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);

        Transaction transaction = new Transaction(
                id,
                "EXPENSE",
                amountVal,
                category,
                "Paid through " + source,
                category,
                pref.getFamilyId(),
                pref.getUserId(),
                Utils.getCurrentTimestamp(),
                "",
                0.0,
                "",
                "completed",
                true,
                null,
                "",
                iconRes,
                bgColor,
                iconColor,
                System.currentTimeMillis()
        );
        
        transactionRepository.saveLocally(transaction);
        showLoading(false);
        showToast(getString(R.string.msg_expense_saved), Status.SUCCESS);
        
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
