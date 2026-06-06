package com.upreyvan.carti.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.databinding.FragmentOnboardingOptionsBinding;
import com.upreyvan.carti.ui.family.JoinFamilyFragment;
import com.upreyvan.carti.util.Utils;

public class OnboardingOptionsFragment extends BaseFragment<FragmentOnboardingOptionsBinding> {
    private OnboardingViewModel viewModel;

    @Override
    protected FragmentOnboardingOptionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingOptionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        setupDynamicPadding();
        observeViewModel();
        
        viewModel.fetchUserStatus();
        
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        if (!pref.isEmployed()) {
            getBinding().cardCreate.setVisibility(View.GONE);
        }

        getBinding().cardCreate.setOnClickListener(v -> navigateTo(new OnboardingCreateFragment()));
        getBinding().cardJoin.setOnClickListener(v -> navigateTo(new JoinFamilyFragment()));

        getBinding().btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), MainActivity.class));
            requireActivity().finish();
        });
    }

    private void observeViewModel() {
        viewModel.getUserStatus().observe(getViewLifecycleOwner(), userDoc -> {
            if (userDoc == null) return;
            
            String familyId = String.valueOf(userDoc.get("familyId"));
            if (familyId != null && !familyId.isEmpty() && !"null".equals(familyId)) {
                startActivity(new Intent(requireActivity(), MainActivity.class));
                requireActivity().finish();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::setLoading);
    }

    private void setLoading(boolean isLoading) {
        getBinding().cardCreate.setEnabled(!isLoading);
        getBinding().cardJoin.setEnabled(!isLoading);
        getBinding().btnSkip.setEnabled(!isLoading);
    }

    @Override
    protected void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    )
                    .replace(R.id.start_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().onboardingOptionsHeader,
                getBinding().onboardingOptions,
                0f,
                0
        );
    }
}
