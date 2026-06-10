package com.upreyvan.carti.ui.profile;

import android.content.Intent;
import android.net.Uri;
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
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void setupInfo() {
        String version;
        try {
            version = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "1.0.0";
        }
        getBinding().tvVersion.setText(getString(R.string.app_version_format, version));
        
        getBinding().btnTerms.setOnClickListener(v -> openUrl(getString(R.string.url_terms)));
        getBinding().btnCredits.setOnClickListener(v -> {
            boolean isExpanded = getBinding().tvCreditsContent.getVisibility() == View.VISIBLE;
            getBinding().tvCreditsContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            getBinding().tvCreditsContent.setText(getString(R.string.credits_full_desc));
            getBinding().ivCreditsArrow.animate().rotation(isExpanded ? 0 : 90).start();
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            showToast("Unable to open link", com.upreyvan.carti.utils.UiHelper.Status.ERROR);
        }
    }
}
