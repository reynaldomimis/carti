package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentInviteFamilyBinding;

public class InviteFamilyFragment extends BaseFragment<FragmentInviteFamilyBinding> {

    @Override
    protected FragmentInviteFamilyBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentInviteFamilyBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupContent();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.invite_family_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
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
            Toast.makeText(requireContext(), "Family ID Copied!", Toast.LENGTH_SHORT).show();
        });

        getBinding().btnShare.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Opening Share Sheet...", Toast.LENGTH_SHORT).show();
        });
    }
}