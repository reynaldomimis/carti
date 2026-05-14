package com.upreyvan.carti.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentOnboardingOptionsBinding;
import com.upreyvan.carti.ui.family.JoinFamilyFragment;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.data.remote.AppwriteManager;
import io.appwrite.models.User;
import java.util.Map;
import android.widget.Toast;

public class OnboardingOptionsFragment extends BaseFragment<FragmentOnboardingOptionsBinding> {

    @Override
    protected FragmentOnboardingOptionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingOptionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDynamicPadding();
        checkIfAlreadyInFamily();
        
        com.upreyvan.carti.data.local.PreferenceManager pref = new com.upreyvan.carti.data.local.PreferenceManager(requireContext());
        if (!pref.isEmployed()) {
            getBinding().cardCreate.setVisibility(View.GONE);
            // Optional: If they are not employee, maybe they should automatically see the Join screen
            // navigateTo(new JoinFamilyFragment()); 
        }

        getBinding().cardCreate.setOnClickListener(v -> navigateTo(new OnboardingCreateFragment()));
        getBinding().cardJoin.setOnClickListener(v -> navigateTo(new JoinFamilyFragment()));

        getBinding().btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), MainActivity.class));
            requireActivity().finish();
        });
    }

    private void checkIfAlreadyInFamily() {
        setLoading(true);
        AppwriteManager.getInstance(requireContext()).getUser(new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(User<Map<String, Object>> result) {
                if (isAdded()) {
                    // In some Appwrite versions it's getId()
                    fetchUserDocument();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    setLoading(false);
                }
            }
        });
    }

    private void fetchUserDocument() {
        new com.upreyvan.carti.data.remote.ApiHelper(requireContext()).sync("", "2000-01-01", "2099-12-31", new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (isAdded()) {
                    setLoading(false);
                    Map<String, Object> summary = (Map<String, Object>) result.get("summary");
                    if (summary != null && summary.get("balance") != null) {
                        // If balance exists, it means the family document was successfully fetched
                        startActivity(new Intent(requireActivity(), MainActivity.class));
                        requireActivity().finish();
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    setLoading(false);
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        getBinding().cardCreate.setEnabled(!isLoading);
        getBinding().cardJoin.setEnabled(!isLoading);
        getBinding().btnSkip.setEnabled(!isLoading);
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
                getBinding().onboardingOptionsHeader,
                getBinding().onboardingOptions,
                0f,
                0
        );
    }
}