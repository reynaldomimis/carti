package com.upreyvan.carti.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class AmountTextWatcher implements TextWatcher {

    private final EditText editText;
    private String current = "";
    private CharSequence originalHint;

    public AmountTextWatcher(EditText editText) {
        this.editText = editText;
        this.originalHint = editText.getHint();
        this.editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Hide hint and clear text to avoid "placeholder play" while typing
                editText.setHint("");
                editText.setText("");
            } else {
                // Restore hint when focus is lost
                if (editText.getText().length() == 0) {
                    editText.setHint(originalHint);
                }
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        String original = s.toString();
        if (original.equals(current)) return;

        // 1. Save cursor position relative to digits and decimal point
        int selectionStart = editText.getSelectionStart();
        int contentCharsBefore = 0;
        for (int i = 0; i < selectionStart && i < original.length(); i++) {
            char c = original.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                contentCharsBefore++;
            }
        }

        editText.removeTextChangedListener(this);

        // Standard Setup: Clean input to allow digits and a single decimal point
        String cleanString = original.replaceAll("[^\\d.]", "");
        
        // Handle decimal point logic to prevent multiple dots and limit to 2 decimal places
        if (cleanString.contains(".")) {
            int dotIndex = cleanString.indexOf('.');
            String integerPart = cleanString.substring(0, dotIndex);
            String decimalPart = cleanString.substring(dotIndex + 1).replace(".", "");
            if (decimalPart.length() > 2) {
                decimalPart = decimalPart.substring(0, 2);
            }
            cleanString = integerPart + "." + decimalPart;
        }

        if (!cleanString.isEmpty()) {
            if (cleanString.equals(".")) {
                current = ".";
                return; 
            }
            try {
                double parsed = Double.parseDouble(cleanString);
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
                
                // Only show decimals if the number is not a whole number or if user is currently typing decimals
                if (parsed % 1 == 0 && !original.contains(".")) {
                    formatter.setMinimumFractionDigits(0);
                } else {
                    formatter.setMinimumFractionDigits(0);
                    formatter.setMaximumFractionDigits(2);
                }
                
                String formatted = formatter.format(parsed);
                
                // Handle visual transition for trailing decimal point
                if (original.endsWith(".") && !formatted.contains(".")) {
                    formatted += ".";
                }

                current = formatted;
                editText.setText(formatted);
                
                // 2. Restore cursor position based on content characters
                int newSelection = 0;
                int contentCharsCount = 0;
                while (newSelection < formatted.length() && contentCharsCount < contentCharsBefore) {
                    char c = formatted.charAt(newSelection);
                    if (Character.isDigit(c) || c == '.') {
                        contentCharsCount++;
                    }
                    newSelection++;
                }
                
                // Ensure cursor is not placed before the currency symbol if it was after it
                editText.setSelection(Math.min(newSelection, formatted.length()));
                
            } catch (NumberFormatException e) {
                // Ignore
            }
        } else {
            current = "";
            editText.setText("");
        }

        editText.addTextChangedListener(this);
    }
}
