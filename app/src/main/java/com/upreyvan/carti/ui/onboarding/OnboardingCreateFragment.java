package com.upreyvan.carti.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentOnboardingCreateBinding;
import com.upreyvan.carti.util.Utils;

import java.util.Map;

import com.upreyvan.carti.util.Validator;

public class OnboardingCreateFragment extends BaseFragment<FragmentOnboardingCreateBinding> {

    @Override
    protected FragmentOnboardingCreateBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingCreateBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        displayUserRole();
        getBinding().btnCreate.setOnClickListener(v -> performCreateFamily());
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
        setLoading(true);
        
        new ApiHelper(requireContext()).createFamily(familyName, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (isAdded()) {
                    setLoading(false);
                    
                    PreferenceManager pref = PreferenceManager.getInstance(requireContext());
                    Object fid = result.get("familyId");
                    String familyId = (fid != null && !"null".equals(String.valueOf(fid))) ? String.valueOf(fid) : "";
                    if (!familyId.isEmpty()) {
                        pref.setFamilyId(familyId);
                        pref.setAdminId(pref.getUserId());
                    }

                    Object ic = result.get("inviteCode");
                    String inviteCode = (ic != null && !"null".equals(String.valueOf(ic))) ? String.valueOf(ic) : "";
                    if (!inviteCode.isEmpty()) {
                        pref.setInviteCode(inviteCode);
                    }
                    
                    String displayCode = inviteCode;
                    if (displayCode.startsWith("FAM-")) {
                        displayCode = displayCode.replace("FAM-", "");
                    }
                    navigateTo(OnboardingStatusFragment.newInstance(displayCode));
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    setLoading(false);
                    String message = error.getMessage();
                    if ("ALREADY_IN_A_FAMILY".equals(message)) {
                        showToast(R.string.err_already_in_family, com.upreyvan.carti.util.ToastHelper.Status.WARNING);
                        startActivity(new Intent(requireActivity(), MainActivity.class));
                        requireActivity().finish();
                    } else if ("PARENTS_ONLY".equals(message)) {
                        showToast(R.string.err_parents_only, com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                    } else {
                        showToast(getString(R.string.err_error_prefix, message), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                    }
                }
            }
        });
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
