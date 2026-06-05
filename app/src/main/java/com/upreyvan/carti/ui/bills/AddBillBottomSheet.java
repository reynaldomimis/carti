package com.upreyvan.carti.ui.bills;

import com.upreyvan.carti.util.ToastHelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.ArrayAdapter;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddBillBinding;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Utils;
import java.util.Map;

public class AddBillBottomSheet extends BaseBottomSheetFragment<LayoutBottomSheetAddBillBinding> {

    private static final String ARG_DATE = "arg_date";
    private ApiHelper apiHelper;
    private String formattedDate;

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
        apiHelper = new ApiHelper(requireContext());

        if (getArguments() != null) {
            formattedDate = getArguments().getString(ARG_DATE);
        }
        getBinding().etDueDate.setText(formattedDate);

        setupDropdowns();
        setupListeners();
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
            String amount = getBinding().etAmount.getText() != null ? getBinding().etAmount.getText().toString().trim() : "";
            String category = getBinding().actCategory.getText().toString();

            if (billName.isEmpty() || amount.isEmpty()) {
                ToastHelper.show(requireContext(), R.string.msg_fill_all_fields, UiHelper.Status.ERROR);
                return;
            }

            String title = "BILL: " + billName;
            String content = "A new bill for " + category + " (" + Utils.formatCurrency(Double.parseDouble(amount)) + ") is due on " + formattedDate;

            apiHelper.sendAnnouncement(title, content, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override public void onSuccess(Map<String, Object> result) {}
                @Override public void onError(Throwable error) {}
            });

            ToastHelper.show(requireContext(), R.string.msg_bill_saved, UiHelper.Status.SUCCESS);
            dismiss();
        });
    }
}


