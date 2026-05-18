package com.upreyvan.carti.ui.family;

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
import com.upreyvan.carti.databinding.FragmentJoinFamilyBinding;
import com.upreyvan.carti.util.Utils;

import java.util.Map;

import com.upreyvan.carti.util.Validator;

public class JoinFamilyFragment extends BaseFragment<FragmentJoinFamilyBinding> {

    @Override
    protected FragmentJoinFamilyBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentJoinFamilyBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        displayUserRole();
        setupListeners();
    }

    private void displayUserRole() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        getBinding().tvUserRole.setText(pref.getUserRole());
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().joinFamilyHeader,
                getBinding().joinFamilyRoot,
                0f,
                0
        );
    }

    private void setupListeners() {
        getBinding().btnJoin.setOnClickListener(v -> {
            if (!checkNetwork()) return;

            if (Validator.isEmpty(getBinding().etFamilyId)) {
                getBinding().tilFamilyId.setError(getString(R.string.err_invalid_family_id));
                return;
            }

            String inviteCode = getBinding().etFamilyId.getText().toString().trim();
            setLoading(true);
            new ApiHelper(requireContext()).joinFamily(inviteCode, new AppwriteManager.AppwriteCallback<>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    if (isAdded()) {
                        setLoading(false);
                        PreferenceManager pref = new PreferenceManager(requireContext());
                        pref.setOnboardingFinished(true);
                        
                        String familyId = String.valueOf(result.get("familyId"));
                        if (familyId != null && !"null".equals(familyId)) {
                            pref.setFamilyId(familyId);
                        }

                        showToast(R.string.msg_join_success, com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
                        Intent intent = new Intent(requireActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
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
                        } else if ("INVALID_INVITE_CODE".equals(message)) {
                            getBinding().tilFamilyId.setError(getString(R.string.err_invalid_invite_code));
                        } else {
                            showToast(getString(R.string.err_error_prefix, message), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                        }
                    }
                }
            });
        });
    }

    private void setLoading(boolean isLoading) {
        getBinding().btnJoin.setEnabled(!isLoading);
        getBinding().btnJoin.setText(isLoading ? getString(R.string.btn_joining) : getString(R.string.btn_join_family));
    }
}