package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActionBottomSheet;
import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;
import com.upreyvan.carti.util.StringHelper;

public class BillDetailsBottomSheet extends BaseActionBottomSheet {

    private static final String ARG_BILL_ID = "arg_bill_id";
    private static final String ARG_BILL_NAME = "arg_bill_name";
    
    private String notificationId;
    private String billName;
    private BillsViewModel viewModel;

    public static BillDetailsBottomSheet newInstance(String notificationId, String name) {
        BillDetailsBottomSheet fragment = new BillDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_BILL_ID, notificationId);
        args.putString(ARG_BILL_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onSetupUI(LayoutBaseActionBottomSheetBinding binding) {
        viewModel = new ViewModelProvider(this).get(BillsViewModel.class);
        Bundle args = getArguments();
        if (args != null) {
            notificationId = args.getString(ARG_BILL_ID);
            billName = args.getString(ARG_BILL_NAME);
            binding.etNotes.setText(String.format("Payment for %s", billName));
        }
        
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                showSuccess("Bill paid and transaction recorded!");
                dismiss();
            }
        });
        viewModel.getIsLoading().observe(this, loading -> setActionEnabled(!loading));
    }

    @Override
    protected String getTitle() {
        return billName != null ? billName : getString(R.string.mark_as_paid);
    }

    @Override
    protected String getSubtitle() {
        return "Verify the amount before marking as paid.";
    }

    @Override
    protected String getButtonText() {
        return getString(R.string.mark_as_paid);
    }

    @Override
    protected boolean isAmountRequired() {
        return true;
    }

    @Override
    protected boolean isNotesVisible() {
        return true;
    }

    @Override
    protected void onActionClicked(String amount, String notes) {
        if (amount.isEmpty()) {
            showError("Please enter the amount");
            return;
        }

        double amountVal = StringHelper.parseDouble(amount);
        viewModel.payBill(notificationId, billName, amountVal, notes);
    }
}
