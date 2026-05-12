package com.upreyvan.carti.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentOnboardingCreateBinding;
import com.upreyvan.carti.util.Utils;

public class OnboardingCreateFragment extends BaseFragment<FragmentOnboardingCreateBinding> {

    @Override
    protected FragmentOnboardingCreateBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingCreateBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDynamicPadding();
        setupSpinner();

        getBinding().btnCreate.setOnClickListener(v -> navigateTo(new OnboardingStatusFragment()));
    }

    private void setupSpinner() {
        String[] roles = {
                getString(R.string.role_father),
                getString(R.string.role_mother),
                getString(R.string.role_sibling),
                getString(R.string.role_child)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles);
        getBinding().spinnerRole.setAdapter(adapter);
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
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
                getBinding().onboardingCreateHeader,
                getBinding().onboardingCreate,
                0f,
                0
        );
    }
}