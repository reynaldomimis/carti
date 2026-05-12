package com.upreyvan.carti.ui.family;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.FragmentInviteFamilyBinding;
import com.upreyvan.carti.util.Utils;

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
        getBinding().tvValidity.setText(getString(R.string.validity_format, getString(R.string.mock_validity_date)));

        // Setup Steps
        getBinding().step1.tvStepNumber.setText("1");
        getBinding().step1.tvStepDescription.setText(R.string.step_1);

        getBinding().step2.tvStepNumber.setText("2");
        getBinding().step2.tvStepDescription.setText(R.string.step_2);

        getBinding().step3.tvStepNumber.setText("3");
        getBinding().step3.tvStepDescription.setText(R.string.step_3);

        getBinding().btnCopy.setOnClickListener(v -> {
            Toast.makeText(this, "Family ID Copied!", Toast.LENGTH_SHORT).show();
        });

        getBinding().btnShare.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Share Sheet...", Toast.LENGTH_SHORT).show();
        });
    }
}
