package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityAddBillBinding;
import com.upreyvan.carti.data.repository.BillRepository;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.data.local.PreferenceManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AddBillActivity extends BaseActivity<ActivityAddBillBinding> {

    private BillRepository billRepository;

    @Override
    protected ActivityAddBillBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddBillBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billRepository = new BillRepository(this);
        setupToolbar();
        setupStatusBar();
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
            if (Validator.isEmpty(getBinding().etBillName) || Validator.isEmpty(getBinding().etAmount)) {
                showToast(getString(R.string.msg_fill_all_fields), com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                return;
            }

            String name = getBinding().etBillName.getText().toString();
            String amount = "₱" + getBinding().etAmount.getText().toString();
            String familyId = new PreferenceManager(this).getFamilyId();
            String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
            
            Bill newBill = new Bill(
                    UUID.randomUUID().toString(),
                    familyId,
                    name,
                    date,
                    amount,
                    "Unpaid",
                    R.drawable.ic_calendar
            );

            billRepository.saveLocally(newBill);
            showToast(getString(R.string.msg_bill_saved), com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
            finish();
        });
    }
}
