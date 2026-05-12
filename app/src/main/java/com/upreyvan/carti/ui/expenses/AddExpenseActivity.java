package com.upreyvan.carti.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.ExpenseManager;
import com.upreyvan.carti.databinding.ActivityAddExpenseBinding;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;

public class AddExpenseActivity extends BaseActivity<ActivityAddExpenseBinding> {

    @Override
    protected ActivityAddExpenseBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddExpenseBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupDropdowns();
        setupClickListeners();
    }

    private void setupDropdowns() {
        // Dynamic Categories from CategoryManager
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

        // Payment Sources
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
        getBinding().etSource.setText(sources[0], false); // Default to Cash
        
        // Default Date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        getBinding().tvDate.setText(sdf.format(calendar.getTime()));
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
        getBinding().btnDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Expense Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(selection);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                getBinding().tvDate.setText(sdf.format(calendar.getTime()));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        getBinding().btnSave.setOnClickListener(v -> {
            String amount = getBinding().etAmount.getText().toString();
            String category = getBinding().etCategory.getText().toString();
            String source = getBinding().etSource.getText().toString();

            if (amount.isEmpty() || category.isEmpty() || source.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to ExpenseManager
            double amountVal = Double.parseDouble(amount);
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());
            
            // Get Category Icon
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
            int bgColor = (selectedCategory != null) ? getColor(selectedCategory.getBackgroundColor()) : getColor(R.color.log_others);

            Transaction transaction = new Transaction(
                    category,
                    time,
                    "₱" + String.format(Locale.getDefault(), "%.2f", amountVal),
                    iconRes,
                    bgColor,
                    iconColor
            );
            ExpenseManager.getInstance().addTransaction(transaction);

            Toast.makeText(this, "Expense Saved!", Toast.LENGTH_SHORT).show();
            
            // Return to MainActivity and clear the stack (removes AddOptionsActivity)
            Intent intent = new Intent(this, com.upreyvan.carti.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
