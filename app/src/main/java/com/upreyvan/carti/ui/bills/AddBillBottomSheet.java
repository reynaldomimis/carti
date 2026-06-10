package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddBillBinding;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.utils.StringHelper;
import com.upreyvan.carti.utils.ToastHelper;
import com.upreyvan.carti.utils.UiHelper;
import com.upreyvan.carti.utils.Utils;

import java.util.List;

public class AddBillBottomSheet extends BaseBottomSheetFragment<LayoutBottomSheetAddBillBinding> {

    private static final String ARG_DATE = "arg_date";
    private String formattedDate;
    private BillsViewModel viewModel;

    public static AddBillBottomSheet newInstance(String formattedDate) {
        AddBillBottomSheet fragment = new AddBillBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, formattedDate);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected LayoutBottomSheetAddBillBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutBottomSheetAddBillBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BillsViewModel.class);

        if (getArguments() != null) {
            formattedDate = getArguments().getString(ARG_DATE);
        }
        getBinding().etDueDate.setText(formattedDate);

        setupDropdowns();
        setupListeners();
        setupInputValidation();
        observeViewModel();
        validateForm();
    }

    private void setupInputValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateForm(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        getBinding().etBillName.addTextChangedListener(validationWatcher);
        getBinding().etAmount.addTextChangedListener(new com.upreyvan.carti.utils.AmountTextWatcher(getBinding().etAmount));
        getBinding().etAmount.addTextChangedListener(validationWatcher);
        getBinding().actCategory.addTextChangedListener(validationWatcher);
        getBinding().etDueDate.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        String name = getBinding().etBillName.getText().toString().trim();
        String amountStr = getBinding().etAmount.getText().toString().trim();
        String date = getBinding().etDueDate.getText().toString().trim();
        double amount = StringHelper.parseDouble(amountStr);
        
        boolean isAmountEntered = !amountStr.isEmpty();
        boolean isValid = !name.isEmpty() && isAmountEntered && amount > 0 && !date.isEmpty();
        boolean isLoading = viewModel.getIsLoading().getValue() != null && viewModel.getIsLoading().getValue();

        getBinding().btnSave.setEnabled(isValid && !isLoading);

        if (isAmountEntered && amount <= 0) {
            getBinding().layoutAmount.setError("Please enter a valid amount");
        } else if (!isAmountEntered) {
            getBinding().layoutAmount.setError("Amount is required");
        } else {
            getBinding().layoutAmount.setError(null);
            getBinding().layoutAmount.setErrorEnabled(false);
        }
    }

    private void observeViewModel() {
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                ToastHelper.show(requireContext(), R.string.msg_bill_saved, UiHelper.Status.SUCCESS);
                dismiss();
            }
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> validateForm());
    }

    private void setupDropdowns() {
        String[] categories = {"Water", "Electricity", "Internet/Wifi", "Load", "Rent", "Others"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        getBinding().actCategory.setAdapter(catAdapter);
        getBinding().actCategory.setText(categories[0], false);

        String[] statuses = {getString(R.string.status_active), "Pending", "Paid"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, statuses);
        getBinding().actStatus.setAdapter(statusAdapter);
        getBinding().actStatus.setText(statuses[0], false);
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());

        getBinding().etDueDate.setOnClickListener(v -> {
            com.google.android.material.datepicker.MaterialDatePicker<Long> picker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Due Date")
                    .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            
            picker.addOnPositiveButtonClickListener(selection -> {
                formattedDate = Utils.formatDate(selection);
                getBinding().etDueDate.setText(formattedDate);
            });
            picker.show(getChildFragmentManager(), "DATE_PICKER");
        });

        getBinding().btnSave.setOnClickListener(v -> {
            String billName = getBinding().etBillName.getText() != null ? getBinding().etBillName.getText().toString().trim() : "";
            String amountStr = getBinding().etAmount.getText() != null ? getBinding().etAmount.getText().toString().trim() : "";
            String category = getBinding().actCategory.getText().toString();

            if (billName.isEmpty() || amountStr.isEmpty()) {
                UiHelper.showSnackbar(getBinding().getRoot(), R.string.msg_fill_all_fields, UiHelper.Status.ERROR);
                return;
            }

            List<Bill> currentBills = viewModel.getBills().getValue();
            if (currentBills != null) {
                for (Bill b : currentBills) {
                    if (b.getName().equalsIgnoreCase(billName)) {
                        UiHelper.showSnackbar(getBinding().getRoot(), "This bill already exists", UiHelper.Status.WARNING);
                        return;
                    }
                }
            }

            String title = "BILL: " + billName;
            String content = "A new bill for " + category + " (" + Utils.formatCurrency(StringHelper.parseDouble(amountStr)) + ") is due on " + formattedDate;

            viewModel.saveBill(title, content);
        });
    }
}
