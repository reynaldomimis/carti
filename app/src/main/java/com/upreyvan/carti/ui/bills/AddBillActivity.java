package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityAddBillBinding;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.data.local.PreferenceManager;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AddBillActivity extends BaseActivity<ActivityAddBillBinding> {

    private TransactionRepository transactionRepository;

    @Override
    protected ActivityAddBillBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddBillBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(this);
        setupToolbar();
        setupStatusBar();
        setupCategoryDropdown();
        setupClickListeners();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.title_add_bill);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupStatusBar() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnSave,
                1f,
                20
        );
    }

    private void setupClickListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().etBillName.getText().toString();
            String amountText = getBinding().etAmount.getText().toString();
            String category = getBinding().actCategory.getText().toString();

            if (Validator.isEmpty(getBinding().etBillName) || Validator.isEmpty(getBinding().etAmount) || category.isEmpty()) {
                showToast(getString(R.string.msg_fill_all_fields), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                return;
            }

            double amount = Double.parseDouble(amountText);
            String familyId = new PreferenceManager(this).getFamilyId();
            String userId = new PreferenceManager(this).getUserId();
            String date = Utils.formatDateShort(Calendar.getInstance());
            
            Transaction transaction = new Transaction();
            transaction.setTitle(name);
            transaction.setAmount(amount);
            transaction.setType("EXPENSE");
            transaction.setCategory(category);
            transaction.setFamilyId(familyId);
            transaction.setUserId(userId);
            transaction.setCreatedAt(date);
            transaction.setPaid(false);

            transactionRepository.addTransaction(transaction, null);
            showToast(getString(R.string.msg_bill_saved), com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
            finish();
        });
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Water", "Electricity", "Internet/Wifi", "Load", "Rent", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        getBinding().actCategory.setAdapter(adapter);
        getBinding().actCategory.setText(categories[0], false);
    }
}
