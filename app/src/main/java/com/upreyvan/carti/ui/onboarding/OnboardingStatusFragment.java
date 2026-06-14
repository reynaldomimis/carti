package com.upreyvan.carti.ui.onboarding;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentOnboardingStatusBinding;

import java.util.Map;

public class OnboardingStatusFragment extends BaseFragment<FragmentOnboardingStatusBinding> {

    private static final String ARG_INVITE_CODE = "invite_code";
    private static final String ARG_IS_WAITING = "is_waiting_approval";
    private static final String ARG_IS_DECLINED = "is_declined";
    private OnboardingViewModel viewModel;

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

    public static OnboardingStatusFragment newInstanceForDeclined() {
        OnboardingStatusFragment fragment = new OnboardingStatusFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DECLINED, true);
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
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        handleArguments();
        observeViewModel();

        getBinding().btnCopy.setOnClickListener(v -> {
            if (getBinding() != null) {
                String code = getBinding().tvInviteCode.getText().toString();
                copyToClipboard(code);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUserStatus().observe(getViewLifecycleOwner(), userDoc -> {
            if (userDoc == null) return;
            
            String familyId = com.upreyvan.carti.utils.ValueHelper.toStr(userDoc.get("familyId"));
            String pendingFamilyId = com.upreyvan.carti.utils.ValueHelper.toStr(userDoc.get("pendingFamilyId"));

            if (!familyId.isEmpty()) {
                handleApproved(userDoc);
            } else if (pendingFamilyId.isEmpty() || "declined".equals(pendingFamilyId)) {
                handleDeclined();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> showLoading(loading));
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) showToast(error, com.upreyvan.carti.utils.UiHelper.Status.ERROR);
        });
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Invite Code", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            showToast("Invite code copied!", com.upreyvan.carti.utils.UiHelper.Status.SUCCESS);
        }
    }

    private void handleArguments() {
        Bundle args = getArguments();
        if (args == null) return;

        boolean isWaiting = args.getBoolean(ARG_IS_WAITING, false);
        boolean isDeclined = args.getBoolean(ARG_IS_DECLINED, false);

        if (isDeclined) {
            handleDeclined();
        } else if (isWaiting) {
            getBinding().tvTitle.setText("Waiting for Approval");
            getBinding().tvDescription.setText("Your request has been sent. Please wait for the Family Head's approval.");
            getBinding().layoutInviteCode.setVisibility(View.GONE);
            
            // Hide button while waiting as requested, realtime listener will handle the transition
            getBinding().btnStatus.setVisibility(View.GONE);
            getBinding().btnCopy.setVisibility(View.GONE);

            RealtimeRepository.getInstance(requireContext()).startListening();
            startRealtimeListener();
        } else {
            String inviteCode = args.getString(ARG_INVITE_CODE);
            getBinding().tvTitle.setText("Family Created!");
            getBinding().tvDescription.setText("Share this code with your family members.");
            getBinding().tvInviteCode.setText(inviteCode);
            getBinding().layoutInviteCode.setVisibility(View.VISIBLE);
            getBinding().btnStatus.setText("Go to Home");
            getBinding().btnStatus.setOnClickListener(v -> finishOnboarding());
        }
    }

    private void startRealtimeListener() {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        String userId = pref.getUserId();
        if (userId.isEmpty()) return;

        RealtimeRepository.getInstance(requireContext()).getUserUpdateStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                // Check if the update is for the current user
                String updateUserId = com.upreyvan.carti.utils.ValueHelper.toStr(payload.get("$id"));
                if (!userId.equals(updateUserId)) return;

                String familyId = com.upreyvan.carti.utils.ValueHelper.toStr(payload.get("familyId"));
                String pendingFamilyId = com.upreyvan.carti.utils.ValueHelper.toStr(payload.get("pendingFamilyId"));

                if (!familyId.isEmpty()) {
                    // Approved! Refresh user status to trigger handleApproved
                    viewModel.fetchUserStatus();
                } else if (pendingFamilyId.isEmpty() || "declined".equals(pendingFamilyId)) {
                    // Declined or reset - show declined screen
                    handleDeclined();
                }
            }
        });
    }

    private void handleApproved(Map<String, Object> user) {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        pref.saveUser(user);
        
        getBinding().tvTitle.setText("Welcome to the Family!");
        getBinding().tvDescription.setText("Your request has been approved!");
        
        getBinding().btnStatus.setText("Go to Home");
        getBinding().btnStatus.setVisibility(View.VISIBLE);
        getBinding().btnStatus.setOnClickListener(v -> finishOnboarding());
        
        new android.os.Handler().postDelayed(() -> {
            if (isAdded()) finishOnboarding();
        }, 1500);
    }

    private void handleDeclined() {
        getBinding().tvTitle.setText("Request Declined");
        getBinding().tvDescription.setText("We're sorry, but your request was declined. You can try joining another family or create your own.");
        getBinding().layoutInviteCode.setVisibility(View.GONE);
        
        getBinding().btnStatus.setText("Try New Code");
        getBinding().btnStatus.setVisibility(View.VISIBLE);
        getBinding().btnStatus.setOnClickListener(v -> {
            PreferenceManager pref = PreferenceManager.getInstance(requireContext());
            pref.setOnboardingFinished(false);
            
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.start_fragment_container, new com.upreyvan.carti.ui.family.JoinFamilyFragment())
                        .commit();
            }
        });

        getBinding().btnCopy.setText("Create Family");
        getBinding().btnCopy.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.start_fragment_container, new OnboardingCreateFragment())
                        .commit();
            }
        });
        getBinding().btnCopy.setVisibility(View.VISIBLE);
    }

    private void finishOnboarding() {
        PreferenceManager.getInstance(requireContext()).setOnboardingFinished(true);
        Intent intent = new Intent(requireActivity(), com.upreyvan.carti.ui.auth.SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
