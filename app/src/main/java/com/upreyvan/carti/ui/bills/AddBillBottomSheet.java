package com.upreyvan.carti.ui.bills;

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
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddBillBinding;
import com.upreyvan.carti.util.ToastHelper;
import java.util.Map;

public class AddBillBottomSheet extends BaseBottomSheetFragment<LayoutBottomSheetAddBillBinding> {

    private static final String ARG_DATE = "arg_date";
    private TransactionRepository transactionRepository;
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
        transactionRepository = TransactionRepository.getInstance(requireContext());
        pref = new PreferenceManager(requireContext());

        formattedDate = getArguments().getString(ARG_DATE);
        getBinding().tvTitle.setText(getString(R.string.add_bill_at_date, formattedDate));

        setupCategoryDropdown();

        getBinding().btnSave.setOnClickListener(v -> {
            String amountStr = getBinding().etBillAmount.getText().toString().trim();
            String category = getBinding().actCategory.getText().toString();

            if (amountStr.isEmpty() || category.isEmpty()) {
                ToastHelper.show(requireContext(), R.string.msg_fill_all_fields, ToastHelper.Status.ERROR);
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String familyId = pref.getFamilyId();
            String userId = pref.getUserId();

            Transaction transaction = new Transaction();
            transaction.setTitle(category);
            transaction.setAmount(amount);
            transaction.setType("EXPENSE");
            transaction.setCategory(category);
            transaction.setFamilyId(familyId);
            transaction.setUserId(userId);
            transaction.setCreatedAt(formattedDate);
            transaction.setPaid(false);

            transactionRepository.addTransaction(transaction, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    // Success is handled by Repository's sync logic
                }

                @Override
                public void onError(Throwable error) {
                    // Handle error if needed
                }
            });

            ToastHelper.show(requireContext(), R.string.msg_bill_saved, ToastHelper.Status.SUCCESS);
            dismiss();
        });
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Water", "Electricity", "Internet/Wifi", "Load", "Rent", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        getBinding().actCategory.setAdapter(adapter);
        
        // Default selection
        getBinding().actCategory.setText(categories[0], false);
    }
}
