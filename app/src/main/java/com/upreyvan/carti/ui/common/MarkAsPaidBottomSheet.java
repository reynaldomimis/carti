package com.upreyvan.carti.ui.common;

import android.os.Bundle;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActionBottomSheet;
import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;

public class MarkAsPaidBottomSheet extends BaseActionBottomSheet {

    private OnPaymentConfirmedListener listener;
    private String initialAmount;

    public interface OnPaymentConfirmedListener {
        void onConfirmed(String amount, String notes);
    }

    public static MarkAsPaidBottomSheet newInstance(String amount, OnPaymentConfirmedListener listener) {
        MarkAsPaidBottomSheet fragment = new MarkAsPaidBottomSheet();
        fragment.initialAmount = amount;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    protected String getTitle() {
        return getString(R.string.mark_as_paid);
    }

    @Override
    protected String getSubtitle() {
        return "Confirm the payment details below.";
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
        return true; // Show notes by default for Mark as Paid
    }

    @Override
    protected void onSetupUI(LayoutBaseActionBottomSheetBinding binding) {
        if (initialAmount != null) {
            // Remove currency symbol for editing
            String cleanAmount = initialAmount.replace("₱", "").replace(",", "").trim();
            binding.etAmount.setText(cleanAmount);
        }
    }

    @Override
    protected void onActionClicked(String amount, String notes) {
        if (amount.isEmpty()) {
            showError("Please enter the paid amount");
            return;
        }

        if (listener != null) {
            listener.onConfirmed(amount, notes);
        }
        showSuccess("Payment marked as successful");
    }
}
