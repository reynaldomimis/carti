package com.upreyvan.carti.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentProfileBinding;
import com.upreyvan.carti.model.ProfileMenuItem;
import com.upreyvan.carti.ui.auth.LoginActivity;
import com.upreyvan.carti.ui.debt.DebtTrackerFragment;
import com.upreyvan.carti.ui.family.MembersFragment;
import com.upreyvan.carti.ui.goals.GoalFragment;
import com.upreyvan.carti.ui.track.IncomeModeFragment;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.DocumentList;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileMenuAdapter adapter;
    private List<ProfileMenuItem> menuItems;
    private ApiHelper apiHelper;

    @Override
    protected FragmentProfileBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiHelper = new ApiHelper(requireContext());
        setupToolbar();
        setupUserInfo();
        setupMenuItems();
        fetchMemberCount();
        
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().profileScroll, 1f, 20);
    }

    private void setupUserInfo() {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        getBinding().tvUserName.setText(pref.getUsername());
        getBinding().tvUserEmail.setText(pref.getUserEmail());
        getBinding().btnLogout.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        showLoading(true, "Logging out...");
        AppwriteManager.getInstance(requireContext()).logout(new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(Object result) { finishLogout(); }
            @Override public void onError(Throwable error) { finishLogout(); }
        });
    }

    private void finishLogout() {
        new Thread(() -> {
            AppDatabase.getInstance(requireContext()).clearAllTables();
            requireActivity().runOnUiThread(() -> {
                PreferenceManager.getInstance(requireContext()).clear();
                showLoading(false);
                startActivity(new Intent(requireContext(), LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                requireActivity().finish();
            });
        }).start();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.profile_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity main) {
                main.navigateTo(Constants.Navigation.HOME);
            }
        });
    }

    private void setupMenuItems() {
        menuItems = new ArrayList<>();
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_myplaces, R.string.menu_family, getString(R.string.menu_family_sub), MembersFragment.class));
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
                    android.util.Log.e("CARTI_DEBUG", "Fragment creation failed", e);
                }
            }
        });
        adapter.submitList(menuItems);
        getBinding().rvProfileMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvProfileMenu.setAdapter(adapter);
    }

    private void fetchMemberCount() {
        apiHelper.getMembers(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (isAdded() && result.getDocuments() != null) {
                    updateFamilyMemberCount(result.getDocuments().size());
                }
            }
            @Override public void onError(Throwable error) {}
        });
    }

    private void updateFamilyMemberCount(int count) {
        if (menuItems == null || menuItems.isEmpty()) return;
        ProfileMenuItem oldItem = menuItems.get(0);
        ProfileMenuItem newItem = new ProfileMenuItem(oldItem.getIconResId(), oldItem.getTitleResId(), getString(R.string.menu_family_sub_format, count), oldItem.getFragmentClass());
        menuItems.set(0, newItem);
        adapter.submitList(new ArrayList<>(menuItems));
    }

    @Override
    protected void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() instanceof MainActivity main) {
            main.setBottomNavVisibility(false);
        }
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
