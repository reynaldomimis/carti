package com.upreyvan.carti.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.adapters.ProfileMenuAdapter;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityProfileBinding;
import com.upreyvan.carti.fragments.AboutFragment;
import com.upreyvan.carti.fragments.BackupSyncFragment;
import com.upreyvan.carti.fragments.FamilyChatFragment;
import com.upreyvan.carti.fragments.GoalFragment;
import com.upreyvan.carti.fragments.InviteFamilyFragment;
import com.upreyvan.carti.fragments.MembersFragment;
import com.upreyvan.carti.fragments.SalaryModeFragment;
import com.upreyvan.carti.fragments.SettingsFragment;
import com.upreyvan.carti.model.ProfileMenuItem;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BaseActivity<ActivityProfileBinding> {

    @Override
    protected ActivityProfileBinding inflateBinding(LayoutInflater inflater) {
        return ActivityProfileBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDynamicPadding();
        setupToolbar();
        setupMenuItems();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.profile_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.GONE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupMenuItems() {
        List<ProfileMenuItem> menuItems = new ArrayList<>();

        int memberCount = 4;
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_myplaces, R.string.menu_family, getString(R.string.menu_family_sub_format, memberCount), new MembersFragment()));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_add, R.string.menu_invite, getString(R.string.menu_invite_sub), new InviteFamilyFragment()));
        menuItems.add(new ProfileMenuItem(android.R.drawable.stat_notify_chat, R.string.menu_chat, getString(R.string.menu_chat_sub), new FamilyChatFragment()));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_compass, R.string.menu_goals, getString(R.string.menu_goals_sub), new GoalFragment()));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_today, R.string.menu_salary, getString(R.string.menu_salary_sub), new SalaryModeFragment()));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_popup_sync, R.string.menu_backup, getString(R.string.menu_backup_sub), new BackupSyncFragment()));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_manage, R.string.menu_settings, getString(R.string.menu_settings_sub), new SettingsFragment()));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_help, R.string.menu_help, getString(R.string.menu_help_sub), null));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_info_details, R.string.menu_about, getString(R.string.menu_about_sub), new AboutFragment(), false));

        ProfileMenuAdapter adapter = new ProfileMenuAdapter(menuItems, item -> {
            if (item.getFragment() != null) {
                navigateTo(item.getFragment());
            }
        });

        getBinding().rvProfileMenu.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvProfileMenu.setAdapter(adapter);
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().profileScroll,
                1f,
                20
        );
    }

}
