package com.upreyvan.carti.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;
import com.upreyvan.carti.util.ToastHelper;

/**
 * A reusable Base Bottom Sheet for common actions like "Mark as Paid" or "Update Amount".
 * Centralizes the UI for title, amount input, and notes.
 */
public abstract class BaseActionBottomSheet extends BaseBottomSheetFragment<LayoutBaseActionBottomSheetBinding> {

    @Override
    protected LayoutBaseActionBottomSheetBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutBaseActionBottomSheetBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupBaseUI();
        onSetupUI(getBinding());
        
        getBinding().btnAction.setOnClickListener(v -> {
            String amount = getBinding().etAmount.getText().toString().trim();
            String notes = getBinding().etNotes.getText().toString().trim();
            onActionClicked(amount, notes);
        });
    }

    private void setupBaseUI() {
        // Default visibility settings
        getBinding().tilAmount.setVisibility(isAmountRequired() ? View.VISIBLE : View.GONE);
        getBinding().tilNotes.setVisibility(isNotesVisible() ? View.VISIBLE : View.GONE);
        
        getBinding().tvTitle.setText(getTitle());
        if (getSubtitle() != null) {
            getBinding().tvSubtitle.setVisibility(View.VISIBLE);
            getBinding().tvSubtitle.setText(getSubtitle());
        }
        
        getBinding().btnAction.setText(getButtonText());
    }

    // Configuration methods to be overridden by child classes
    protected abstract String getTitle();
    protected String getSubtitle() { return null; }
    protected abstract String getButtonText();
    protected boolean isAmountRequired() { return false; }
    protected boolean isNotesVisible() { return false; }

    /**
     * Optional hook for additional UI setup
     */
    protected void onSetupUI(LayoutBaseActionBottomSheetBinding binding) {}

    /**
     * Action to perform when the primary button is clicked
     */
    protected abstract void onActionClicked(String amount, String notes);

    protected void showSuccess(String message) {
        ToastHelper.show(requireContext(), message, ToastHelper.Status.SUCCESS);
        dismiss();
    }

    protected void showError(String message) {
        ToastHelper.show(requireContext(), message, ToastHelper.Status.ERROR);
    }
}
