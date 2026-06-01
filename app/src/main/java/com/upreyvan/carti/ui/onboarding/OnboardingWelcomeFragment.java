package com.upreyvan.carti.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentOnboardingWelcomeBinding;
import com.upreyvan.carti.ui.family.JoinFamilyFragment;
import com.upreyvan.carti.util.Utils;
import java.util.Map;

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
        apiHelper.getUser(new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> userDoc) {
                if (!isAdded()) return;
                setLoading(false);

                boolean isEmployed = false;
                Object emp = userDoc.get("isEmployed");
                if (emp != null) {
                    if (emp instanceof Boolean) isEmployed = (Boolean) emp;
                    else isEmployed = Boolean.parseBoolean(String.valueOf(emp));
                }

                String familyId = String.valueOf(userDoc.get("familyId"));
                if (familyId == null || "null".equals(familyId)) familyId = "";
                
                String pendingFamilyId = String.valueOf(userDoc.get("pendingFamilyId"));
                if (pendingFamilyId == null || "null".equals(pendingFamilyId)) pendingFamilyId = "";
                
                String userId = String.valueOf(userDoc.getOrDefault("$id", ""));

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

                if (!familyId.isEmpty()) {
                    pref.setOnboardingFinished(true);
                    Utils.showToast(requireContext(), "Welcome back!");
                    startActivity(new Intent(requireActivity(), MainActivity.class));
                    requireActivity().finish();
                } else if ("declined".equals(pendingFamilyId) || !pendingFamilyId.isEmpty()) {
                    navigateTo(OnboardingStatusFragment.newInstanceForWaiting());
                } else if (isEmployed) {
                    navigateTo(new OnboardingOptionsFragment());
                } else {
                    navigateTo(new JoinFamilyFragment());
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                setLoading(false);
                PreferenceManager pref = new PreferenceManager(requireContext());
                boolean isEmployed = pref.isEmployed();
                Toast.makeText(requireContext(), getString(R.string.debug_fallback_is_employed, isEmployed), Toast.LENGTH_LONG).show();
                if (isEmployed) navigateTo(new OnboardingOptionsFragment());
                else navigateTo(new JoinFamilyFragment());
            }
        });
    }

    private void setLoading(boolean isLoading) {
        getBinding().btnStart.setEnabled(!isLoading);
        getBinding().btnStart.setText(isLoading ? getString(R.string.msg_checking_status) : getString(R.string.btn_start));
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().onboardingWelcomeHeader, getBinding().onboardingWelcome, 0f, 0);
    }

    @Override
    protected void navigateTo(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.start_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
