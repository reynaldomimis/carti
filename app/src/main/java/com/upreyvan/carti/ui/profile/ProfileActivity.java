package com.upreyvan.carti.ui.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.ui.auth.LoginActivity;
import com.upreyvan.carti.ui.profile.ProfileMenuAdapter;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityProfileBinding;
import com.upreyvan.carti.ui.family.InviteFamilyActivity;
import com.upreyvan.carti.ui.profile.AboutFragment;
import com.upreyvan.carti.ui.family.FamilyChatFragment;
import com.upreyvan.carti.ui.goals.GoalFragment;
import com.upreyvan.carti.ui.debt.DebtTrackerFragment;
import com.upreyvan.carti.ui.family.MembersFragment;
import com.upreyvan.carti.ui.track.IncomeModeFragment;
import com.upreyvan.carti.ui.profile.SettingsFragment;
import com.upreyvan.carti.model.ProfileMenuItem;
import com.upreyvan.carti.util.Utils;

import com.upreyvan.carti.data.remote.ApiHelper;
import io.appwrite.models.DocumentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends BaseActivity<ActivityProfileBinding> {

    private ProfileMenuAdapter adapter;
    private List<ProfileMenuItem> menuItems;
    private ApiHelper apiHelper;

    @Override
    protected ActivityProfileBinding inflateBinding(LayoutInflater inflater) {
        return ActivityProfileBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apiHelper = new ApiHelper(this);
        setupDynamicPadding();
        setupToolbar();
        setupUserInfo();
        setupMenuItems();
        fetchMemberCount();
    }

    private void setupUserInfo() {
        PreferenceManager pref = new PreferenceManager(this);
        getBinding().tvUserName.setText(pref.getUsername());
        getBinding().tvUserEmail.setText(pref.getUserEmail());
        getBinding().btnLogout.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        showLoading(true, "Logging out...");

        AppwriteManager.getInstance(this).logout(new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Object result) {
                finishLogout();
            }

            @Override
            public void onError(Throwable error) {
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        new Thread(() -> {
            AppDatabase.getInstance(this).clearAllTables();
            runOnUiThread(() -> {
                new PreferenceManager(this).clear();
                showLoading(false);
                navigateToLogin();
            });
        }).start();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.profile_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.GONE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> goBackToHome());
    }

    private void goBackToHome() {
        Intent intent = new Intent(this, com.upreyvan.carti.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("show_home", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            goBackToHome();
        }
    }

    private void setupMenuItems() {
        menuItems = new ArrayList<>();

        // Initial default (will be updated by fetchMemberCount)
        String membersSubtitle = getString(R.string.menu_family_sub); 
        
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_myplaces, R.string.menu_family, membersSubtitle, MembersFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_compass, R.string.menu_goals, getString(R.string.menu_goals_sub), GoalFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_edit, R.string.menu_debt, getString(R.string.menu_debt_sub), DebtTrackerFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_today, R.string.menu_salary, getString(R.string.menu_salary_sub), IncomeModeFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_lock_idle_lock, R.string.menu_security, getString(R.string.menu_security_sub), SecurityFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_manage, R.string.menu_settings, getString(R.string.menu_settings_sub), SettingsFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_help, R.string.menu_help, getString(R.string.menu_help_sub), HelpFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_info_details, R.string.menu_about, getString(R.string.menu_about_sub), AboutFragment.class, false));

        adapter = new ProfileMenuAdapter(item -> {
            if (item.getFragmentClass() != null) {
                try {
                    navigateTo(item.getFragmentClass().getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    android.util.Log.e("CARTI_DEBUG", "Fragment instantiation failed", e);
                }
            }
        });
        adapter.submitList(menuItems);

        getBinding().rvProfileMenu.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvProfileMenu.setAdapter(adapter);
    }

    private void fetchMemberCount() {
        apiHelper.getMembers(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (result.getDocuments() != null) {
                    int count = result.getDocuments().size();
                    updateFamilyMemberCount(count);
                }
            }

            @Override
            public void onError(Throwable error) {
                // Keep default if failed
            }
        });
    }

    private void updateFamilyMemberCount(int count) {
        if (menuItems == null || menuItems.isEmpty()) return;

        ProfileMenuItem oldItem = menuItems.get(0);
        ProfileMenuItem newItem = new ProfileMenuItem(
                oldItem.getIconResId(),
                oldItem.getTitleResId(),
                getString(R.string.menu_family_sub_format, count),
                oldItem.getFragmentClass()
        );

        menuItems.set(0, newItem);
        runOnUiThread(() -> adapter.submitList(new ArrayList<>(menuItems)));
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, fragment)
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
