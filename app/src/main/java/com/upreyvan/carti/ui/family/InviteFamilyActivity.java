package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentInviteFamilyBinding;
import com.upreyvan.carti.util.Utils;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import io.appwrite.models.Document;
import java.util.Map;

public class InviteFamilyActivity extends BaseActivity<FragmentInviteFamilyBinding> {

    private ApiHelper apiHelper;

    @Override
    protected FragmentInviteFamilyBinding inflateBinding(LayoutInflater inflater) {
        return FragmentInviteFamilyBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiHelper = new ApiHelper(this);
        setupDynamicPadding();
        setupToolbar();
        setupContent();
        fetchInviteCode();
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
        PreferenceManager pref = PreferenceManager.getInstance(this);
        String inviteCode = pref.getInviteCode();

        updateInviteUI(inviteCode);

        getBinding().step1.tvStepNumber.setText("1");
        getBinding().step1.tvStepDescription.setText(R.string.step_1);

        getBinding().step2.tvStepNumber.setText("2");
        getBinding().step2.tvStepDescription.setText(R.string.step_2);

        getBinding().step3.tvStepNumber.setText("3");
        getBinding().step3.tvStepDescription.setText(R.string.step_3);
    }

    private void fetchInviteCode() {
        apiHelper.getFamilySummary(new AppwriteManager.AppwriteCallback<Document<Map<String, Object>>>() {
            @Override
            public void onSuccess(Document<Map<String, Object>> result) {
                if (result.getData() != null) {
                    Object code = result.getData().get("inviteCode");
                    if (code != null) {
                        String inviteCode = String.valueOf(code);
                        // Save to prefs for next time
                        PreferenceManager.getInstance(InviteFamilyActivity.this).setInviteCode(inviteCode);
                        runOnUiThread(() -> updateInviteUI(inviteCode));
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                // Keep existing UI
            }
        });
    }

    private void updateInviteUI(String inviteCode) {
        if (inviteCode == null || inviteCode.isEmpty()) {
            inviteCode = PreferenceManager.getInstance(this).getFamilyId(); // Fallback
        }

        if (inviteCode != null && inviteCode.startsWith("FAM-")) {
            inviteCode = inviteCode.replace("FAM-", "");
        }

        String finalInviteCode = inviteCode != null ? inviteCode : "";
        getBinding().tvFamilyId.setText(finalInviteCode);
        getBinding().tvValidity.setText(getString(R.string.validity_format, getString(R.string.label_never)));

        getBinding().btnCopy.setOnClickListener(v -> {
            copyToClipboard(finalInviteCode);
        });

        getBinding().btnShare.setOnClickListener(v -> {
            shareInviteCode(finalInviteCode);
        });
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.label_family_id), text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.msg_invite_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareInviteCode(String code) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.msg_share_invite, code));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_invite)));
    }
}
