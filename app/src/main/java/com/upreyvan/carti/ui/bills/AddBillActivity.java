package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.databinding.ActivityAddBillBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionType;
import com.upreyvan.carti.utils.UiHelper.Status;
import com.upreyvan.carti.utils.Utils;
import com.upreyvan.carti.utils.Validator;
import java.util.Calendar;

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
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().btnSave, 1f, 20);
    }

    private void setupClickListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().actCategory.getText().toString();
            String amountText = getBinding().etAmount.getText().toString();
            String category = getBinding().actCategory.getText().toString();

            if (category.isEmpty() || Validator.isEmpty(getBinding().etAmount)) {
                showToast(getString(R.string.msg_fill_all_fields), Status.WARNING);
                return;
            }

            double amount = Double.parseDouble(amountText);
            PreferenceManager pref = PreferenceManager.getInstance(this);
            String familyId = pref.getFamilyId();
            String userId = pref.getUserId();
            String date = Utils.formatDateShort(Calendar.getInstance());
            
            Transaction transaction = new Transaction();
            transaction.setTitle(name);
            transaction.setAmount(amount);
            transaction.setCategory(category);
            transaction.setFamilyId(familyId);
            transaction.setUserId(userId);
            transaction.setCreatedAt(date);
            transaction.setPaid(false);

            transactionRepository.createItem(TransactionType.EXPENSE, transaction, null);
            showToast(getString(R.string.msg_bill_saved), Status.SUCCESS);
            finish();
        });
    }

    private void setupCategoryDropdown() {
        java.util.List<com.upreyvan.carti.models.Category> allCategories = com.upreyvan.carti.managers.CategoryManager.getInstance(this).getCategories();
        java.util.List<String> billSubcategories = new java.util.ArrayList<>();
        for (com.upreyvan.carti.models.Category c : allCategories) {
            if ("Bills".equalsIgnoreCase(c.getParentCategory())) {
                billSubcategories.add(c.getName());
            }
        }
        
        if (billSubcategories.isEmpty()) {
            billSubcategories = java.util.Arrays.asList("Water", "Electricity", "Internet/Wifi", "Load/Data", "Rent", "Others");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, billSubcategories);
        getBinding().actCategory.setAdapter(adapter);
        if (!billSubcategories.isEmpty()) {
            getBinding().actCategory.setText(billSubcategories.get(0), false);
        }
    }
}

