package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddGoalBinding;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class AddGoalBottomSheetFragment extends BaseBottomSheetFragment<LayoutBottomSheetAddGoalBinding> {

    public static AddGoalBottomSheetFragment newInstance() {
        return new AddGoalBottomSheetFragment();
    }

    @Override
    protected LayoutBottomSheetAddGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutBottomSheetAddGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDatePicker();
        setupListeners();
    }

    private void setupDatePicker() {
        View.OnClickListener show = v -> showDatePicker();
        getBinding().etTargetDate.setOnClickListener(show);
        getBinding().tilTargetDate.setEndIconOnClickListener(show);
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
            getBinding().etTargetDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(c.getTime()));
        });
        dp.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnSaveGoal.setOnClickListener(v -> onSaveClicked());
    }

    private void onSaveClicked() {
        if (!checkNetwork()) return;
        if (Validator.isEmpty(getBinding().etGoalName) || Validator.isEmpty(getBinding().etTargetAmount)) {
            showToast(R.string.msg_fill_all_fields, ToastHelper.Status.WARNING);
            return;
        }

        String name = getBinding().etGoalName.getText().toString().trim();
        double amount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String date = getBinding().etTargetDate.getText().toString().trim();

        showLoading(true, "Saving goal...");
        TransactionRepository.getInstance(requireContext()).addGoal(name, amount, date, null, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (isAdded()) {
                    showLoading(false);
                    showToast(R.string.msg_goal_saved_success, ToastHelper.Status.SUCCESS);
                    dismiss();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    showLoading(false);
                    showToast(getString(R.string.err_generic, error.getMessage()), ToastHelper.Status.ERROR);
                }
            }
        });
    }
}
