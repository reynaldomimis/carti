package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.databinding.FragmentInviteFamilyBinding;
import com.upreyvan.carti.util.Utils;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

public class InviteFamilyActivity extends BaseActivity<FragmentInviteFamilyBinding> {

    @Override
    protected FragmentInviteFamilyBinding inflateBinding(LayoutInflater inflater) {
        return FragmentInviteFamilyBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupContent();
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnShare,
                1f,
                0
        );
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.invite_family_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupContent() {
        PreferenceManager pref = new PreferenceManager(this);
        String inviteCode = pref.getFamilyId();
        if (inviteCode != null && inviteCode.startsWith("FAM-")) {
            inviteCode = inviteCode.replace("FAM-", "");
        }

        getBinding().tvFamilyId.setText(inviteCode != null ? inviteCode : "");
        getBinding().tvValidity.setText(getString(R.string.validity_format, getString(R.string.mock_validity_date)));

        // Setup Steps
        getBinding().step1.tvStepNumber.setText("1");
        getBinding().step1.tvStepDescription.setText(R.string.step_1);

        getBinding().step2.tvStepNumber.setText("2");
        getBinding().step2.tvStepDescription.setText(R.string.step_2);

        getBinding().step3.tvStepNumber.setText("3");
        getBinding().step3.tvStepDescription.setText(R.string.step_3);

        String finalInviteCode = inviteCode;
        getBinding().btnCopy.setOnClickListener(v -> {
            copyToClipboard(finalInviteCode);
        });

        getBinding().btnShare.setOnClickListener(v -> {
            shareInviteCode(finalInviteCode);
        });
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Family ID", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Family ID Copied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareInviteCode(String code) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Join my family on Carti! Use this Family ID: " + code);
        startActivity(Intent.createChooser(intent, "Share Family ID"));
    }
}
