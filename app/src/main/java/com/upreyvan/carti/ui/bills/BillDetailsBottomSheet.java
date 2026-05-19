package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActionBottomSheet;
import com.upreyvan.carti.data.repository.BillRepository;
import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;

public class BillDetailsBottomSheet extends BaseActionBottomSheet {

    private static final String ARG_BILL_ID = "arg_bill_id";
    private static final String ARG_BILL_NAME = "arg_bill_name";
    private static final String ARG_BILL_AMOUNT = "arg_bill_amount";
    
    private BillRepository billRepository;
    private String billId;
    private String billName;
    private String billAmount;

    public static BillDetailsBottomSheet newInstance(String billId, String name, String amount) {
        BillDetailsBottomSheet fragment = new BillDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_BILL_ID, billId);
        args.putString(ARG_BILL_NAME, name);
        args.putString(ARG_BILL_AMOUNT, amount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onSetupUI(LayoutBaseActionBottomSheetBinding binding) {
        billRepository = new BillRepository(requireContext());
        Bundle args = getArguments();
        if (args != null) {
            billId = args.getString(ARG_BILL_ID);
            billName = args.getString(ARG_BILL_NAME);
            billAmount = args.getString(ARG_BILL_AMOUNT);
            
            if (billAmount != null) {
                String cleanAmount = billAmount.replace("₱", "").replace(",", "").trim();
                binding.etAmount.setText(cleanAmount);
            }
        }
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
        return true; // Notes field is now available
    }

    @Override
    protected void onActionClicked(String amount, String notes) {
        if (amount.isEmpty()) {
            showError("Please enter the amount");
            return;
        }
        
        // Logic to update bill status and perhaps add the notes to the transaction
        billRepository.updateStatus(billId, getString(R.string.status_paid_label));
        showSuccess("Bill marked as paid successfully");
    }
}
