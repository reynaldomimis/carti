package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentOnboardingRolesBinding;
import com.upreyvan.carti.util.Utils;

public class OnboardingRolesFragment extends BaseFragment<FragmentOnboardingRolesBinding> {

    @Override
    protected FragmentOnboardingRolesBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingRolesBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDynamicPadding();
        setupRoles();

        getBinding().btnNext.setOnClickListener(v -> navigateTo(new OnboardingOptionsFragment()));
    }

    private void setupRoles() {
        getBinding().roleTatay.tvRoleName.setText(R.string.role_father);
        getBinding().roleNanay.tvRoleName.setText(R.string.role_mother);
        getBinding().roleKuya.tvRoleName.setText(R.string.role_sibling);
        getBinding().roleAnak.tvRoleName.setText(R.string.role_child);
        getBinding().roleIba.tvRoleName.setText(R.string.role_others);
        getBinding().roleIba.ivRoleIcon.setImageResource(android.R.drawable.ic_menu_more);
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.start_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().onboardingRolesHeader,
                getBinding().onboardingRoles,
                0f,
                0
        );
    }
}