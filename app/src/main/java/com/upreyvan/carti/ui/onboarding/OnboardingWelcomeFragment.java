package com.upreyvan.carti.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentOnboardingWelcomeBinding;
import com.upreyvan.carti.util.Utils;

import java.util.Map;
import android.widget.Toast;

public class OnboardingWelcomeFragment extends BaseFragment<FragmentOnboardingWelcomeBinding> {

    @Override
    protected FragmentOnboardingWelcomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOnboardingWelcomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        
        getBinding().btnStart.setOnClickListener(v -> fetchLatestUserStatus());
    }

    private void fetchLatestUserStatus() {
        setLoading(true);
        
        ApiHelper apiHelper = new ApiHelper(requireContext());
        apiHelper.getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> userDoc) {
                if (!isAdded()) return;
                setLoading(false);

                // Get live status from backend
                boolean isEmployed = false;
                Object emp = userDoc.get("isEmployed");
                if (emp != null) {
                    if (emp instanceof Boolean) isEmployed = (Boolean) emp;
                    else isEmployed = Boolean.parseBoolean(String.valueOf(emp));
                }

                String familyId = String.valueOf(userDoc.get("familyId"));
                if (familyId == null || "null".equals(familyId)) familyId = "";
                String userId = String.valueOf(userDoc.getOrDefault("$id", ""));

                // Update preferences to match remote user data
                PreferenceManager pref = new PreferenceManager(requireContext());
                pref.setUserData(
                    String.valueOf(userDoc.get("username")),
                    String.valueOf(userDoc.get("email")),
                    String.valueOf(userDoc.get("role")),
                    isEmployed,
                    familyId,
                    "",
                    userId
                );

                Toast.makeText(requireContext(), getString(R.string.debug_live_is_employed, isEmployed), Toast.LENGTH_SHORT).show();

                if (isEmployed) {
                    navigateTo(new OnboardingOptionsFragment());
                } else {
                    navigateTo(new com.upreyvan.carti.ui.family.JoinFamilyFragment());
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                setLoading(false);
                
                // Fallback to Prefs if network/backend fails
                PreferenceManager pref = new PreferenceManager(requireContext());
                boolean isEmployed = pref.isEmployed();
                
                Toast.makeText(requireContext(), getString(R.string.debug_fallback_is_employed, isEmployed), Toast.LENGTH_LONG).show();
                
                if (isEmployed) {
                    navigateTo(new OnboardingOptionsFragment());
                } else {
                    navigateTo(new com.upreyvan.carti.ui.family.JoinFamilyFragment());
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        getBinding().btnStart.setEnabled(!isLoading);
        getBinding().btnStart.setText(isLoading ? getString(R.string.msg_checking_status) : getString(R.string.btn_start));
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().onboardingWelcomeHeader,
                getBinding().onboardingWelcome,
                0f,
                0
        );
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
}