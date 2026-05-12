package com.upreyvan.carti.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentNotificationSettingsBinding;

public class NotificationSettingsFragment extends BaseFragment<FragmentNotificationSettingsBinding> {

    @Override
    protected FragmentNotificationSettingsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentNotificationSettingsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupItems();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.notification_settings_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupItems() {
        // Budget Alerts
        getBinding().headerBudget.tvHeader.setText(R.string.section_budget_alerts);
        getBinding().itemDailyBudget.tvTitle.setText(R.string.label_daily_budget_alert);
        getBinding().itemDailyBudget.tvDescription.setText(R.string.desc_daily_budget_alert);
        getBinding().itemDailyBudget.switchWidget.setChecked(true);

        // Salary Reminders
        getBinding().headerSweldo.tvHeader.setText(R.string.section_salary_reminders);
        getBinding().itemSweldoReminder.tvTitle.setText(R.string.label_salary_reminder);
        getBinding().itemSweldoReminder.tvDescription.setText(R.string.desc_salary_reminder);
        getBinding().itemSweldoReminder.switchWidget.setChecked(true);

        // Other
        getBinding().headerOther.tvHeader.setText(R.string.section_other);
        getBinding().itemGoalMilestone.tvTitle.setText(R.string.label_goal_milestone);
        getBinding().itemGoalMilestone.tvDescription.setText(R.string.desc_goal_milestone);
        getBinding().itemGoalMilestone.switchWidget.setChecked(true);

        getBinding().itemDebtReminders.tvTitle.setText(R.string.label_debt_reminders);
        getBinding().itemDebtReminders.tvDescription.setText(R.string.desc_debt_reminders);
        getBinding().itemDebtReminders.switchWidget.setChecked(true);
        getBinding().itemDebtReminders.divider.setVisibility(View.GONE);
    }
}