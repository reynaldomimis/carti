package com.upreyvan.carti.ui.onboarding;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.databinding.FragmentOnboardingStatusBinding;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import java.util.Map;

import io.appwrite.models.RealtimeResponseEvent;
import io.appwrite.models.RealtimeSubscription;

public class OnboardingStatusFragment extends BaseFragment<FragmentOnboardingStatusBinding> {

    private static final String ARG_INVITE_CODE = "invite_code";
    private static final String ARG_IS_WAITING = "is_waiting_approval";
    private RealtimeSubscription subscription = null;

    public static OnboardingStatusFragment newInstance(String inviteCode) {
        OnboardingStatusFragment fragment = new OnboardingStatusFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INVITE_CODE, inviteCode);
        fragment.setArguments(args);
        return fragment;
    }

    public static OnboardingStatusFragment newInstanceForWaiting() {
        OnboardingStatusFragment fragment = new OnboardingStatusFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_WAITING, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentOnboardingStatusBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingStatusBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDynamicPadding();
        handleArguments();

        getBinding().btnCopy.setOnClickListener(v -> {
            String code = getBinding().tvInviteCode.getText().toString();
            copyToClipboard(code);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (subscription != null) {
            subscription.close();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.invite_code_label), text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), R.string.invite_code_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleArguments() {
        if (getArguments() != null) {
            boolean isWaiting = getArguments().getBoolean(ARG_IS_WAITING, false);
            if (isWaiting) {
                getBinding().tvTitle.setText("Waiting for Approval");
                getBinding().tvDescription.setText("Your request to join the family has been sent. Please ask the Family Head to approve your request.");
                getBinding().layoutInviteCode.setVisibility(View.GONE);
                getBinding().btnStatus.setText("Check Status");
                
                getBinding().btnStatus.setOnClickListener(v -> checkApprovalStatus());
                
                // Start Realtime Listening
                startRealtimeListener();
                return;
            }

            String inviteCode = getArguments().getString(ARG_INVITE_CODE);
            if (inviteCode != null && !inviteCode.isEmpty()) {
                getBinding().tvTitle.setText(R.string.onboarding_family_created_title);
                getBinding().tvDescription.setText(R.string.onboarding_family_created_desc);
                getBinding().tvInviteCode.setText(inviteCode);
                getBinding().layoutInviteCode.setVisibility(View.VISIBLE);
                getBinding().btnStatus.setText(R.string.btn_go_to_home);
                
                getBinding().btnStatus.setOnClickListener(v -> {
                    PreferenceManager pref = new PreferenceManager(requireContext());
                    pref.setOnboardingFinished(true);

                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                });
            }
        }
    }

    private void startRealtimeListener() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        String userId = pref.getUserId();
        if (userId.isEmpty()) return;

        RealtimeHelper realtimeHelper = new RealtimeHelper(requireContext());

        subscription = realtimeHelper.subscribeToDocument(
                Constants.Appwrite.COL_USERS,
                userId,
                new RealtimeHelper.RealtimeEventCallback() {
                    @Override
                    public void onEvent(RealtimeResponseEvent<?> event) {
                        if (!isAdded()) return;

                        Map<String, Object> payload = RealtimeHelper.getPayload(event);
                        String familyId = (String) payload.get("familyId");
                        String pendingFamilyId = (String) payload.get("pendingFamilyId");

                        if (familyId != null && !familyId.isEmpty() && !"null".equals(familyId)) {
                            // CASE: APPROVED
                            pref.setFamilyId(familyId);
                            pref.setOnboardingFinished(true);
                            showToast("Request Accepted! Welcome to the family.", com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);

                            Intent intent = new Intent(requireActivity(), MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        } else if (pendingFamilyId == null || pendingFamilyId.isEmpty() || "null".equals(pendingFamilyId)) {
                            // CASE: DENIED
                            getBinding().tvTitle.setText("Request Declined");
                            getBinding().tvDescription.setText("Your request to join the family was declined by the Family Head. Please try another code or create your own family.");
                            getBinding().btnStatus.setText("Return to Join Screen");
                            getBinding().btnStatus.setEnabled(true);
                            getBinding().btnStatus.setOnClickListener(v -> requireActivity().onBackPressed());

                            showToast("Join request was declined.", com.upreyvan.carti.util.ToastHelper.Status.ERROR);

                            if (subscription != null) {
                                subscription.close();
                                subscription = null;
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        Log.e("OnboardingStatus", "Realtime Error: " + error.getMessage());
                    }
                }
        );
    }

    private void checkApprovalStatus() {
        getBinding().btnStatus.setEnabled(false);
        getBinding().btnStatus.setText("Checking...");

        new ApiHelper(requireContext()).getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> user) {
                if (!isAdded()) return;
                
                String familyId = String.valueOf(user.get("familyId"));
                if (familyId != null && !familyId.isEmpty() && !"null".equals(familyId)) {
                    PreferenceManager pref = new PreferenceManager(requireContext());
                    pref.setFamilyId(familyId);
                    pref.setOnboardingFinished(true);

                    showToast("Approved! Welcome to the family.", com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                } else {
                    getBinding().btnStatus.setEnabled(true);
                    getBinding().btnStatus.setText("Check Status");
                    showToast("Still pending approval.", com.upreyvan.carti.util.ToastHelper.Status.INFO);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                getBinding().btnStatus.setEnabled(true);
                getBinding().btnStatus.setText("Check Status");
                showToast("Error: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().onboardingStatusHeader, getBinding().onboardingStatus, 0f, 0);
    }
}
