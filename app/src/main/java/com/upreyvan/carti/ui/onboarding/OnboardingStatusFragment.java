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
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentOnboardingStatusBinding;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;

import java.util.Map;

public class OnboardingStatusFragment extends BaseFragment<FragmentOnboardingStatusBinding> {

    private static final String ARG_INVITE_CODE = "invite_code";
    private static final String ARG_IS_WAITING = "is_waiting_approval";

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
        handleArguments();

        getBinding().btnCopy.setOnClickListener(v -> {
            if (getBinding() != null) {
                String code = getBinding().tvInviteCode.getText().toString();
                copyToClipboard(code);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Invite Code", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            showToast("Invite code copied!", com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
        }
    }

    private void handleArguments() {
        Bundle args = getArguments();
        if (args == null) return;

        boolean isWaiting = args.getBoolean(ARG_IS_WAITING, false);
        if (isWaiting) {
            getBinding().tvTitle.setText("Waiting for Approval");
            getBinding().tvDescription.setText("Ang iyong request ay naisend na. Hintayin ang approval ng Family Head.");
            getBinding().layoutInviteCode.setVisibility(View.GONE);
            getBinding().btnStatus.setText("Check Status");
            getBinding().btnStatus.setOnClickListener(v -> checkApprovalStatus());
            startRealtimeListener();
        } else {
            String inviteCode = args.getString(ARG_INVITE_CODE);
            getBinding().tvTitle.setText("Family Created!");
            getBinding().tvDescription.setText("I-share ang code na ito sa iyong family members.");
            getBinding().tvInviteCode.setText(inviteCode);
            getBinding().layoutInviteCode.setVisibility(View.VISIBLE);
            getBinding().btnStatus.setText("Go to Home");
            getBinding().btnStatus.setOnClickListener(v -> finishOnboarding());
        }
    }

    private void startRealtimeListener() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        String userId = pref.getUserId();
        if (userId.isEmpty()) return;

        RealtimeRepository.getInstance(requireContext()).getUserUpdateStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload == null) return;
            
            String familyId = String.valueOf(payload.get("familyId"));
            String pendingFamilyId = String.valueOf(payload.get("pendingFamilyId"));

            if (familyId != null && !familyId.isEmpty() && !"null".equals(familyId)) {
                pref.setFamilyId(familyId);
                getBinding().tvTitle.setText("Welcome to the Family!");
                getBinding().tvDescription.setText("Your request has been approved. Getting things ready for you...");
                getBinding().btnStatus.setText("Go to Home");
                getBinding().btnStatus.setOnClickListener(v -> finishOnboarding());

                new android.os.Handler().postDelayed(() -> {
                    if (isAdded()) finishOnboarding();
                }, 2000);
            } else if (pendingFamilyId == null || pendingFamilyId.isEmpty() || "null".equals(pendingFamilyId)) {
                getBinding().tvTitle.setText("Request Declined");
                getBinding().tvDescription.setText("We're sorry, but your request to join this family group has been declined by the administrator. Please contact them or try joining a different family.");
                getBinding().btnStatus.setText("Restart to JOIN");
                getBinding().btnStatus.setOnClickListener(v -> {
                    pref.setOnboardingFinished(false);
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                });
            }
        });
    }

    private void checkApprovalStatus() {
        showLoading(true);
        new ApiHelper(requireContext()).getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> user) {
                if (!isAdded()) return;
                showLoading(false);
                String familyId = String.valueOf(user.get("familyId"));
                if (familyId != null && !familyId.isEmpty() && !"null".equals(familyId)) {
                    new PreferenceManager(requireContext()).setFamilyId(familyId);
                    
                    // Show Welcome Success UI
                    getBinding().tvTitle.setText("Welcome to the Family!");
                    getBinding().tvDescription.setText("Your request has been approved. Getting things ready for you...");
                    getBinding().btnStatus.setText("Go to Home");
                    getBinding().btnStatus.setOnClickListener(v -> finishOnboarding());
                    
                    new android.os.Handler().postDelayed(() -> {
                        if (isAdded()) finishOnboarding();
                    }, 2000);
                } else {
                    showToast("Still pending...", com.upreyvan.carti.util.ToastHelper.Status.INFO);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                showLoading(false);
                showError(error);
            }
        });
    }

    private void finishOnboarding() {
        new PreferenceManager(requireContext()).setOnboardingFinished(true);
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
