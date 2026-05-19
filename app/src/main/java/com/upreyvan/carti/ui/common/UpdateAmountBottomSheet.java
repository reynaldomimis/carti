package com.upreyvan.carti.ui.common;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActionBottomSheet;
import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;

public class UpdateAmountBottomSheet extends BaseActionBottomSheet {

    private OnAmountUpdatedListener listener;
    private String initialAmount;
    private String title;

    public interface OnAmountUpdatedListener {
        void onUpdated(String newAmount, String notes);
    }

    public static UpdateAmountBottomSheet newInstance(String title, String currentAmount, OnAmountUpdatedListener listener) {
        UpdateAmountBottomSheet fragment = new UpdateAmountBottomSheet();
        fragment.title = title;
        fragment.initialAmount = currentAmount;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    protected String getTitle() {
        return title != null ? title : "Update Amount";
    }

    @Override
    protected String getButtonText() {
        return getString(R.string.label_update);
    }

    @Override
    protected boolean isAmountRequired() {
        return true;
    }

    @Override
    protected boolean isNotesVisible() {
        return false; // Hidden by default as requested
    }

    @Override
    protected void onSetupUI(LayoutBaseActionBottomSheetBinding binding) {
        if (initialAmount != null) {
            String cleanAmount = initialAmount.replace("₱", "").replace(",", "").trim();
            binding.etAmount.setText(cleanAmount);
        }
    }

    @Override
    protected void onActionClicked(String amount, String notes) {
        if (amount.isEmpty()) {
            showError("Please enter an amount");
            return;
        }

        if (listener != null) {
            listener.onUpdated(amount, notes);
        }
        showSuccess("Amount updated successfully");
    }
}
