package com.upreyvan.carti.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentProfileBinding;
import com.upreyvan.carti.models.ProfileMenuItem;
import com.upreyvan.carti.ui.auth.LoginActivity;
import com.upreyvan.carti.ui.family.MembersFragment;
import com.upreyvan.carti.ui.track.IncomeModeFragment;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileMenuAdapter adapter;
    private List<ProfileMenuItem> menuItems;
    private ProfileViewModel viewModel;

    @Override
    protected FragmentProfileBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        if (getActivity() instanceof MainActivity main) {
            main.setBottomNavVisibility(false);
        }

        setupToolbar();
        setupUserInfo();
        setupMenuItems();
        observeViewModel();
        
        viewModel.fetchMemberCount();
    }

    private void observeViewModel() {
        viewModel.getMemberCount().observe(getViewLifecycleOwner(), this::updateFamilyMemberCount);
        viewModel.getLogoutSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) finishLogout();
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> showLoading(loading, "Logging out..."));
    }

    private void setupUserInfo() {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        getBinding().tvUserName.setText(pref.getUsername());
        getBinding().tvUserEmail.setText(pref.getUserEmail());
        getBinding().btnLogout.setOnClickListener(v -> viewModel.logout());
    }

    private void finishLogout() {
        RealtimeRepository.getInstance(requireContext()).stopListening();
        PreferenceManager.getInstance(requireContext()).clear();
        startActivity(new Intent(requireContext(), LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        requireActivity().finish();
    }

    private void setupToolbar() {
        com.upreyvan.carti.databinding.LayoutCustomToolbarBinding b = getBinding().layoutToolbar;
        b.tvToolbarTitle.setText(R.string.profile_title);
        b.backButtonContainer.setVisibility(View.VISIBLE);
        b.backButtonContainer.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupMenuItems() {
        menuItems = new ArrayList<>();
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_myplaces, R.string.menu_family, getString(R.string.menu_family_sub), MembersFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_today, R.string.menu_salary, getString(R.string.menu_salary_sub), IncomeModeFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_lock_idle_lock, R.string.menu_security, getString(R.string.menu_security_sub), SecurityFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_manage, R.string.menu_settings, getString(R.string.menu_settings_sub), SettingsFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_help, R.string.menu_help, getString(R.string.menu_help_sub), HelpFragment.class));
        menuItems.add(new ProfileMenuItem(android.R.drawable.ic_menu_info_details, R.string.menu_about, getString(R.string.menu_about_sub), AboutFragment.class, false));

        adapter = new ProfileMenuAdapter(item -> {
            if (item.getFragmentClass() == null) return;
            
            androidx.fragment.app.Fragment target;
            Class<?> cls = item.getFragmentClass();
            
            if (cls == MembersFragment.class) target = new MembersFragment();
            else if (cls == IncomeModeFragment.class) target = new IncomeModeFragment();
            else if (cls == SecurityFragment.class) target = new SecurityFragment();
            else if (cls == SettingsFragment.class) target = new SettingsFragment();
            else if (cls == HelpFragment.class) target = new HelpFragment();
            else if (cls == AboutFragment.class) target = new AboutFragment();
            else return;

            navigateTo(target);
        });
        adapter.submitList(menuItems);
        getBinding().rvProfileMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvProfileMenu.setAdapter(adapter);
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
                .add(R.id.fragment_container, fragment)
                .hide(this)
                .addToBackStack(null)
                .commit();
    }
}
