package com.upreyvan.carti.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentSettingsBinding;
import com.upreyvan.carti.ui.notifications.NotificationSettingsFragment;

public class SettingsFragment extends BaseFragment<FragmentSettingsBinding> {

    @Override
    protected FragmentSettingsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentSettingsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupItems();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.settings_title);
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void setupItems() {
        getBinding().headerGeneral.tvHeader.setText(R.string.section_general);

        com.upreyvan.carti.managers.PreferenceManager pref = com.upreyvan.carti.managers.PreferenceManager.getInstance(requireContext());
        boolean isNotifEnabled = pref.isNotificationsEnabled();

        // Notifications
        getBinding().itemNotifications.tvTitle.setText(R.string.label_notifications);
        getBinding().itemNotifications.tvDescription.setText(isNotifEnabled ? R.string.status_on : R.string.status_off);
        getBinding().itemNotifications.switchWidget.setChecked(isNotifEnabled);
        getBinding().itemNotifications.switchWidget.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.setNotificationsEnabled(isChecked);
            getBinding().itemNotifications.tvDescription.setText(isChecked ? R.string.status_on : R.string.status_off);
        });
        getBinding().itemNotifications.getRoot().setOnClickListener(v -> navigateTo(new NotificationSettingsFragment()));

        // Currency
        getBinding().itemCurrency.tvTitle.setText(R.string.label_currency);
        getBinding().itemCurrency.tvStatus.setText(R.string.status_currency_php);

        // Language
        getBinding().itemLanguage.tvTitle.setText(R.string.label_language);
        getBinding().itemLanguage.tvStatus.setText(R.string.status_language_english);
        getBinding().itemLanguage.divider.setVisibility(View.GONE);
    }
}