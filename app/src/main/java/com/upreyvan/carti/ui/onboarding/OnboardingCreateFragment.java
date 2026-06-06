package com.upreyvan.carti.ui.onboarding;

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
import com.upreyvan.carti.databinding.FragmentOnboardingCreateBinding;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;

public class OnboardingCreateFragment extends BaseFragment<FragmentOnboardingCreateBinding> {
    private OnboardingViewModel viewModel;

    @Override
    protected FragmentOnboardingCreateBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingCreateBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        setupDynamicPadding();
        displayUserRole();
        getBinding().btnCreate.setOnClickListener(v -> performCreateFamily());
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getUserStatus().observe(getViewLifecycleOwner(), userDoc -> {
            if (userDoc == null) return;
            
            String inviteCode = String.valueOf(userDoc.get("inviteCode"));
            if (inviteCode == null || "null".equals(inviteCode)) inviteCode = "";
            
            String displayCode = inviteCode;
            if (displayCode.startsWith("FAM-")) {
                displayCode = displayCode.replace("FAM-", "");
            }
            if (!inviteCode.isEmpty()) {
                navigateTo(OnboardingStatusFragment.newInstance(displayCode));
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                if ("ALREADY_IN_A_FAMILY".equals(message)) {
                    showToast(R.string.err_already_in_family, com.upreyvan.carti.util.UiHelper.Status.WARNING);
                    startActivity(new Intent(requireActivity(), com.upreyvan.carti.ui.auth.SplashActivity.class));
                    requireActivity().finish();
                } else if ("PARENTS_ONLY".equals(message)) {
                    showToast(R.string.err_parents_only, com.upreyvan.carti.util.UiHelper.Status.ERROR);
                } else {
                    showToast(getString(R.string.err_error_prefix, message), com.upreyvan.carti.util.UiHelper.Status.ERROR);
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::setLoading);
    }

    private void displayUserRole() {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        String role = pref.getUserRole();
        getBinding().tvUserRole.setText(role);
    }

    private void performCreateFamily() {
        if (Validator.isEmpty(getBinding().etFamilyName)) {
            getBinding().tilFamilyName.setError(getString(R.string.err_required));
            return;
        }
        getBinding().tilFamilyName.setError(null);

        String familyName = getBinding().etFamilyName.getText().toString().trim();
        viewModel.createFamily(familyName);
    }

    private void setLoading(boolean isLoading) {
        getBinding().btnCreate.setEnabled(!isLoading);
        getBinding().btnCreate.setText(isLoading ? getString(R.string.btn_creating) : getString(R.string.btn_create));
    }

    @Override
    protected void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    )
                    .replace(R.id.start_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().onboardingCreateHeader,
                getBinding().onboardingCreate,
                0f,
                0
        );
    }
}
