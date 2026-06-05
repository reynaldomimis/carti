package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActionBottomSheet;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;
import com.upreyvan.carti.util.Constants;

import java.util.Map;

public class BillDetailsBottomSheet extends BaseActionBottomSheet {

    private static final String ARG_BILL_ID = "arg_bill_id";
    private static final String ARG_BILL_NAME = "arg_bill_name";
    
    private String notificationId;
    private String billName;

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
        Bundle args = getArguments();
        if (args != null) {
            notificationId = args.getString(ARG_BILL_ID);
            billName = args.getString(ARG_BILL_NAME);
            
            binding.etNotes.setText(String.format("Payment for %s", billName));
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
        return true;
    }

    @Override
    protected void onActionClicked(String amount, String notes) {
        if (amount.isEmpty()) {
            showError("Please enter the amount");
            return;
        }

        double amountVal = Double.parseDouble(amount);
        ApiHelper apiHelper = new ApiHelper(requireContext());

        apiHelper.addTransaction(amountVal, "EXPENSE", "Bills", billName + ": " + notes, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                AppwriteManager.getInstance(requireContext()).deleteDocument(
                    Constants.Appwrite.DATABASE_ID,
                    Constants.Appwrite.COL_NOTIFICATIONS,
                    notificationId,
                    new AppwriteManager.AppwriteCallback<Object>() {
                        @Override
                        public void onSuccess(Object result) {
                            showSuccess("Bill paid and transaction recorded!");
                            dismiss();
                        }

                        @Override
                        public void onError(Throwable error) {
                            showSuccess("Bill paid, but notification clear failed.");
                            dismiss();
                        }
                    }
                );
            }

            @Override
            public void onError(Throwable error) {
                showError("Failed to record transaction: " + error.getMessage());
            }
        });
    }
}
