package com.upreyvan.carti.base;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.databinding.LayoutBaseActionBottomSheetBinding;
import com.upreyvan.carti.utils.AmountTextWatcher;
import com.upreyvan.carti.utils.UiHelper;

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
        setupValidation();
        
        getBinding().btnAction.setOnClickListener(v -> {
            Editable amountText = getBinding().etAmount.getText();
            Editable notesText = getBinding().etNotes.getText();
            String amountStr = (amountText != null) ? amountText.toString().trim() : "";
            String notes = (notesText != null) ? notesText.toString().trim() : "";
            onActionClicked(amountStr, notes);
        });
        
        validate();
    }

    private void setupValidation() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validate(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        if (isAmountRequired()) {
            getBinding().etAmount.addTextChangedListener(new AmountTextWatcher(getBinding().etAmount));
            getBinding().etAmount.addTextChangedListener(watcher);
        }
    }

    private void validate() {
        if (!isAmountRequired()) {
            setActionEnabled(true);
            return;
        }

        Editable amountText = getBinding().etAmount.getText();
        String amountStr = amountText != null ? amountText.toString().trim() : "";
        double amount = com.upreyvan.carti.utils.StringHelper.parseDouble(amountStr);
        setActionEnabled(amount > 0);
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
