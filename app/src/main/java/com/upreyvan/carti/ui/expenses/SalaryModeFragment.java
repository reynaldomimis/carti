package com.upreyvan.carti.ui.expenses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.databinding.FragmentSalaryModeBinding;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SalaryModeFragment extends BaseFragment<FragmentSalaryModeBinding> {

    @Override
    protected FragmentSalaryModeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentSalaryModeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        updateUI();
        setupListeners();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.salary_mode_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void updateUI() {
        SalaryManager manager = SalaryManager.getInstance(requireContext());
        
        // Next Salary Card
        int daysLeft = manager.getDaysUntilNextPayday();
        getBinding().tvDaysLeft.setText(String.valueOf(daysLeft));
        
        Calendar nextPayday = manager.getNextPayday();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        getBinding().tvTargetDate.setText(getString(R.string.mock_salary_date_format, sdf.format(nextPayday.getTime())));

        // Details
        getBinding().tvSalaryAmount.setText(String.format(Locale.getDefault(), "₱%,.2f", manager.getSalaryAmount()));
        getBinding().tvPaydaySchedule.setText(getString(R.string.payday_format, manager.getFirstPayday(), manager.getSecondPayday()));
        getBinding().tvDailyBudget.setText(String.format(Locale.getDefault(), "₱%,.2f / day", manager.getDailyBudget()));

        // Progress (Simplified logic: assume 15 days cycle)
        int daysPassed = 15 - daysLeft;
        if (daysPassed < 0) daysPassed = 0;
        int progress = (int) ((daysPassed / 15f) * 100);
        getBinding().progressSalary.setProgress(Math.min(progress, 100));
    }

    private void setupListeners() {
        getBinding().btnEditSalary.setOnClickListener(v -> showSalaryEditDialog());
        getBinding().btnEditPayday.setOnClickListener(v -> showPaydayEditDialog());
    }

    private void showSalaryEditDialog() {
        SalaryEditBottomSheet bottomSheet = SalaryEditBottomSheet.newInstance();
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "SalaryEditBottomSheet");
    }

    private void showPaydayEditDialog() {
        PaydayEditBottomSheet bottomSheet = PaydayEditBottomSheet.newInstance();
        bottomSheet.setListener(this::updateUI);
        bottomSheet.show(getChildFragmentManager(), "PaydayEditBottomSheet");
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                null,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_height)
        );
    }
}