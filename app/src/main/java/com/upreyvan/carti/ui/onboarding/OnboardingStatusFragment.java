package com.upreyvan.carti.ui.onboarding;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.upreyvan.carti.databinding.FragmentOnboardingStatusBinding;
import com.upreyvan.carti.util.Utils;

public class OnboardingStatusFragment extends BaseFragment<FragmentOnboardingStatusBinding> {

    private static final String ARG_INVITE_CODE = "invite_code";

    public static OnboardingStatusFragment newInstance(String inviteCode) {
        OnboardingStatusFragment fragment = new OnboardingStatusFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INVITE_CODE, inviteCode);
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

        getBinding().btnStatus.setOnClickListener(v -> {
            PreferenceManager pref = new PreferenceManager(requireContext());
            pref.setOnboardingFinished(true);

            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        getBinding().btnCopy.setOnClickListener(v -> {
            String code = getBinding().tvInviteCode.getText().toString();
            copyToClipboard(code);
        });
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
            String inviteCode = getArguments().getString(ARG_INVITE_CODE);
            if (inviteCode != null && !inviteCode.isEmpty()) {
                getBinding().tvTitle.setText(R.string.onboarding_family_created_title);
                getBinding().tvDescription.setText(R.string.onboarding_family_created_desc);
                getBinding().tvInviteCode.setText(inviteCode);
                getBinding().layoutInviteCode.setVisibility(View.VISIBLE);
                getBinding().btnStatus.setText(R.string.btn_go_to_home);
            }
        }
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().onboardingStatusHeader,
                getBinding().onboardingStatus,
                0f,
                0
        );
    }
}