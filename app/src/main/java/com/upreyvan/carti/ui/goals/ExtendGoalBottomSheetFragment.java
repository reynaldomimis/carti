package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.Gson;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.BottomSheetExtendGoalBinding;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.utils.AmountTextWatcher;
import com.upreyvan.carti.utils.StringHelper;
import com.upreyvan.carti.utils.UiHelper;
import com.upreyvan.carti.utils.Utils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class ExtendGoalBottomSheetFragment extends BaseBottomSheetFragment<BottomSheetExtendGoalBinding> {

    private Transaction goal;
    private TransactionRepository repository;

    public static ExtendGoalBottomSheetFragment newInstance(Transaction goal) {
        ExtendGoalBottomSheetFragment fragment = new ExtendGoalBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("goal_json", new Gson().toJson(goal));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String json = getArguments().getString("goal_json");
            goal = new Gson().fromJson(json, Transaction.class);
        }
        repository = TransactionRepository.getInstance(requireContext());
    }

    @Override
    protected BottomSheetExtendGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetExtendGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupListeners();
    }

    private void setupUI() {
        if (goal.getTargetDate() != null && !goal.getTargetDate().isEmpty()) {
            long millis = com.upreyvan.carti.utils.DateHelper.getMillisFromIso(goal.getTargetDate());
            getBinding().etTargetDate.setText(com.upreyvan.carti.utils.DateHelper.formatDate(millis));
        }
        getBinding().etTargetAmount.setText(String.valueOf(goal.getTargetAmount()));
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());
        
        getBinding().etTargetAmount.addTextChangedListener(new AmountTextWatcher(getBinding().etTargetAmount));

        View.OnClickListener showDate = v -> showDatePicker();
        getBinding().etTargetDate.setOnClickListener(showDate);
        getBinding().tilTargetDate.setEndIconOnClickListener(showDate);

        getBinding().btnUpdate.setOnClickListener(v -> performUpdate());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_target_date)
                .setTheme(R.style.CartiDatePicker)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        dp.addOnPositiveButtonClickListener(s -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(s);
            String newDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(c.getTime());
            getBinding().etTargetDate.setText(newDate);
        });
        dp.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void performUpdate() {
        String newDate = getBinding().etTargetDate.getText().toString().trim();
        String newAmountStr = getBinding().etTargetAmount.getText().toString().trim();
        
        if (newDate.isEmpty()) {
            showToast("Please select a target date", UiHelper.Status.WARNING);
            return;
        }

        if (newAmountStr.isEmpty()) {
            getBinding().tilTargetAmount.setError("Target amount is required");
            return;
        }

        double newAmount = StringHelper.parseDouble(newAmountStr);
        if (newAmount <= 0) {
            getBinding().tilTargetAmount.setError("Please enter a valid amount");
            return;
        }

        if (newAmount < goal.getTargetAmount()) {
            String currentFormatted = Utils.formatCurrency(goal.getTargetAmount());
            getBinding().tilTargetAmount.setError("New target amount cannot be lower than current (" + currentFormatted + ")");
            return;
        }

        getBinding().tilTargetAmount.setError(null);
        showLoading(true, "Updating goal...");
        
        goal.setTargetDate(newDate);
        goal.setTargetAmount(newAmount);
        goal.setStatus("ACTIVE"); 

        repository.updateItem(goal.getType(), goal.getId(), goal, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                showLoading(false);
                showToast("Goal extended successfully", UiHelper.Status.SUCCESS);
                dismiss();
            }

            @Override
            public void onError(Throwable error) {
                showLoading(false);
                showToast("Update failed: " + error.getMessage(), UiHelper.Status.ERROR);
            }
        });
    }
}
