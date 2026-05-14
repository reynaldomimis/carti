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
            String inviteCode = getBinding().etFamilyId.getText().toString().trim();

            if (inviteCode.isEmpty()) {
                getBinding().tilFamilyId.setError(getString(R.string.err_invalid_family_id));
                return;
            }

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

                        Toast.makeText(requireContext(), R.string.msg_join_success, Toast.LENGTH_LONG).show();
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
                            Toast.makeText(requireContext(), R.string.err_already_in_family, Toast.LENGTH_LONG).show();
                            startActivity(new Intent(requireActivity(), MainActivity.class));
                            requireActivity().finish();
                        } else if ("INVALID_INVITE_CODE".equals(message)) {
                            getBinding().tilFamilyId.setError(getString(R.string.err_invalid_invite_code));
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.err_error_prefix, message), Toast.LENGTH_LONG).show();
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