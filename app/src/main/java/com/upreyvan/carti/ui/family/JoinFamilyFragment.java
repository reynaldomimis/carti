package com.upreyvan.carti.ui.family;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentJoinFamilyBinding;
import com.upreyvan.carti.ui.onboarding.OnboardingStatusFragment;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Utils;

import java.util.Map;

public class JoinFamilyFragment extends BaseFragment<FragmentJoinFamilyBinding> {

    @Override
    protected FragmentJoinFamilyBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentJoinFamilyBinding.inflate(inflater, container, false);
    }

    private boolean isJoining = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();

        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        getBinding().tvUserRole.setText(pref.getUserRole());

        getBinding().btnJoin.setOnClickListener(v -> {
            if (isJoining) return;
            
            android.text.Editable text = getBinding().etFamilyId.getText();
            String inviteCode = (text != null) ? text.toString().trim() : "";
            if (inviteCode.isEmpty()) {
                showToast(R.string.error_empty_invite_code, UiHelper.Status.WARNING);
                return;
            }
            joinFamily(inviteCode);
        });
    }

    private void joinFamily(String inviteCode) {
        isJoining = true;
        setLoading(true);
        new ApiHelper(requireContext()).joinFamily(inviteCode, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                    isJoining = false;
                    android.util.Log.d("CARTI_DEBUG", "JoinFamily Success Result: " + result);

                    if (!isAdded() || getActivity() == null) {
                        android.util.Log.e("CARTI_DEBUG", "Fragment not attached to activity!");
                        return;
                    }

                    setLoading(false);
                    PreferenceManager pref = PreferenceManager.getInstance(requireContext());

                    Map<String, Object> data = result;
                    if (result.containsKey("data") && result.get("data") instanceof Map) {
                        //noinspection unchecked
                        data = (Map<String, Object>) result.get("data");
                    }

                    String familyId = data.get("familyId") != null ? String.valueOf(data.get("familyId")) : null;
                    String status = data.get("status") != null ? String.valueOf(data.get("status")) : null;

                    android.util.Log.d("CARTI_DEBUG", "Parsed -> familyId: " + familyId + ", status: " + status);

                    if (familyId != null && !familyId.isEmpty() && !"null".equals(familyId)) {
                        android.util.Log.d("CARTI_DEBUG", "Navigating to MainActivity (Join Immediate)");
                        saveAndFinish(pref, familyId);
                    } else {
                        android.util.Log.d("CARTI_DEBUG", "Navigating to OnboardingStatusFragment (Pending)");
                        navigateTo(OnboardingStatusFragment.newInstanceForWaiting());
                    }
            }

            @Override
            public void onError(Throwable error) {
                isJoining = false;
                if (isAdded()) {
                    setLoading(false);
                    showToast(error.getMessage(), UiHelper.Status.ERROR);
                }
            }
        });
    }

    private void saveAndFinish(PreferenceManager pref, String familyId) {
        pref.setFamilyId(familyId);
        pref.setOnboardingFinished(true);
        
        showToast(R.string.msg_join_success, UiHelper.Status.SUCCESS);
        Intent intent = new Intent(requireActivity(), MainActivity.class);
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
