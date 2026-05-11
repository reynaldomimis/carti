package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAboutBinding;

public class AboutFragment extends BaseFragment<FragmentAboutBinding> {

    @Override
    protected FragmentAboutBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAboutBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupInfo();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.about_app_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupInfo() {
        getBinding().tvVersion.setText(getString(R.string.app_version_format, "1.0.0"));
        
        getBinding().btnTerms.setOnClickListener(v -> {
            // Open Terms
        });

        getBinding().btnPrivacy.setOnClickListener(v -> {
            // Open Privacy
        });
    }
}