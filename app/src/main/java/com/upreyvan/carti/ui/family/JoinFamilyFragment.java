package com.upreyvan.carti.ui.family;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.databinding.FragmentJoinFamilyBinding;
import com.upreyvan.carti.ui.onboarding.OnboardingStatusFragment;
import com.upreyvan.carti.ui.onboarding.OnboardingViewModel;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Utils;

public class JoinFamilyFragment extends BaseFragment<FragmentJoinFamilyBinding> {
    private OnboardingViewModel viewModel;

    @Override
    protected FragmentJoinFamilyBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentJoinFamilyBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        setupDynamicPadding();

        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        getBinding().tvUserRole.setText(pref.getUserRole());

        getBinding().btnJoin.setOnClickListener(v -> {
            android.text.Editable text = getBinding().etFamilyId.getText();
            String inviteCode = (text != null) ? text.toString().trim() : "";
            if (inviteCode.isEmpty()) {
                showToast(R.string.error_empty_invite_code, UiHelper.Status.WARNING);
                return;
            }
            viewModel.joinFamily(inviteCode);
        });
        
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getUserStatus().observe(getViewLifecycleOwner(), userDoc -> {
            if (userDoc == null) return;
            
            String familyId = String.valueOf(userDoc.get("familyId"));
            if (familyId == null || "null".equals(familyId)) familyId = "";
            
            String pendingFamilyId = String.valueOf(userDoc.get("pendingFamilyId"));
            if (pendingFamilyId == null || "null".equals(pendingFamilyId)) pendingFamilyId = "";

            if (!familyId.isEmpty()) {
                saveAndFinish(PreferenceManager.getInstance(requireContext()), familyId);
            } else if ("declined".equals(pendingFamilyId)) {
                navigateTo(OnboardingStatusFragment.newInstanceForDeclined());
            } else {
                navigateTo(OnboardingStatusFragment.newInstanceForWaiting());
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) showToast(error, UiHelper.Status.ERROR);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::setLoading);
    }

    private void saveAndFinish(PreferenceManager pref, String familyId) {
        pref.setFamilyId(familyId);
        pref.setOnboardingFinished(true);
        
        showToast(R.string.msg_join_success, UiHelper.Status.SUCCESS);
        Intent intent = new Intent(requireActivity(), com.upreyvan.carti.ui.auth.SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().joinFamilyRoot, null, 0f, 0);
    }

    private void setLoading(boolean isLoading) {
        getBinding().btnJoin.setEnabled(!isLoading);
        getBinding().etFamilyId.setEnabled(!isLoading);
        getBinding().btnJoin.setText(isLoading ? getString(R.string.btn_joining) : getString(R.string.btn_join_family));
    }
}
