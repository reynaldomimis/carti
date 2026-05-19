package com.upreyvan.carti.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.BillRepository;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddBillBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.util.ToastHelper;

import java.util.UUID;

public class AddBillBottomSheet extends BaseBottomSheetFragment<LayoutBottomSheetAddBillBinding> {

    private static final String ARG_DATE = "arg_date";
    private BillRepository billRepository;
    private PreferenceManager pref;
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
        billRepository = new BillRepository(requireContext());
        pref = new PreferenceManager(requireContext());

        formattedDate = getArguments().getString(ARG_DATE);
        getBinding().tvTitle.setText(getString(R.string.add_bill_at_date, formattedDate));

        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().etBillName.getText().toString().trim();
            String amountStr = getBinding().etBillAmount.getText().toString().trim();

            if (name.isEmpty() || amountStr.isEmpty()) {
                ToastHelper.show(requireContext(), R.string.msg_fill_all_fields, ToastHelper.Status.ERROR);
                return;
            }

            String id = UUID.randomUUID().toString();
            String familyId = pref.getFamilyId();
            String amount = getString(R.string.currency_symbol) + amountStr;
            String status = getString(R.string.status_unpaid);
            int iconResId = R.drawable.ic_calendar;

            Bill newBill = new Bill(id, familyId, name, formattedDate, amount, status, iconResId);
            billRepository.saveLocally(newBill);

            ToastHelper.show(requireContext(), R.string.msg_bill_saved, ToastHelper.Status.SUCCESS);
            dismiss();
        });
    }
}
