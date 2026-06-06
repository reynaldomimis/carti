package com.upreyvan.carti.base;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;
import com.upreyvan.carti.util.UiHelper;

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
            Editable amountText = getBinding().etAmount.getText();
            Editable notesText = getBinding().etNotes.getText();
            String amount = (amountText != null) ? amountText.toString().trim() : "";
            String notes = (notesText != null) ? notesText.toString().trim() : "";
            onActionClicked(amount, notes);
        });
    }

    private void setupBaseUI() {
        getBinding().tilAmount.setVisibility(isAmountRequired() ? View.VISIBLE : View.GONE);
        getBinding().tilNotes.setVisibility(isNotesVisible() ? View.VISIBLE : View.GONE);
        
        getBinding().tvTitle.setText(getTitle());
        if (getSubtitle() != null) {
            getBinding().tvSubtitle.setVisibility(View.VISIBLE);
            getBinding().tvSubtitle.setText(getSubtitle());
        }
        
        getBinding().btnAction.setText(getButtonText());
    }

    protected void setActionEnabled(boolean enabled) {
        if (getBinding() != null) {
            getBinding().btnAction.setEnabled(enabled);
        }
    }

    protected abstract String getTitle();
    protected String getSubtitle() { return null; }
    protected abstract String getButtonText();
    protected boolean isAmountRequired() { return false; }
    protected boolean isNotesVisible() { return false; }
    protected void onSetupUI(LayoutBaseActionBottomSheetBinding binding) {}

    protected abstract void onActionClicked(String amount, String notes);

    protected void showSuccess(String message) {
        UiHelper.showSnackbar(getView(), message, UiHelper.Status.SUCCESS);
        dismiss();
    }

    protected void showError(String message) {
        UiHelper.showSnackbar(getView(), message, UiHelper.Status.ERROR);
    }
}
