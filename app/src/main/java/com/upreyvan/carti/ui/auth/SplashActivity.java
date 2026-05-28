package com.upreyvan.carti.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.ActivitySplashBinding;
import com.upreyvan.carti.ui.onboarding.StartActivity;

import java.util.Map;

import io.appwrite.models.User;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity<ActivitySplashBinding> {

    @Override
    protected ActivitySplashBinding inflateBinding(LayoutInflater inflater) {
        return ActivitySplashBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // ELITE SECURITY: Check if app has been tampered with or re-signed
        com.upreyvan.carti.util.SecurityGuard.checkIntegrity(this);

        super.onCreate(savedInstanceState);
        
        checkSession();
    }

    private void checkSession() {
        ApiHelper apiHelper = new ApiHelper(this);
        apiHelper.getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> userDoc) {
                PreferenceManager pref = new PreferenceManager(SplashActivity.this);
                
                Object fid = userDoc.get("familyId");
                String familyId = (fid != null && !"null".equals(String.valueOf(fid))) ? String.valueOf(fid) : "";
                pref.setFamilyId(familyId);

                if (!familyId.isEmpty()) {
                    pref.setOnboardingFinished(true);
                    navigateToHome();
                } else {
                    pref.setOnboardingFinished(false);
                    navigateToOnboarding();
                }
            }

            @Override
            public void onError(Throwable error) {
                PreferenceManager pref = new PreferenceManager(SplashActivity.this);
                
                String message = error.getMessage();
                boolean isUnauthorized = message != null && (
                        message.contains("Unauthorized") || 
                        message.contains("login") || 
                        message.contains("session")
                );

                if (isUnauthorized) {
                    pref.clear(); // Wipe everything if we are definitely not logged in
                    navigateToLogin();
                } else if (pref.isOnboardingFinished() && !pref.getFamilyId().isEmpty()) {
                    // Likely a network error, allow offline mode if we have data
                    navigateToHome();
                } else {
                    navigateToLogin();
                }
            }
        });
    }

    private void navigateToOnboarding() {
        Intent intent = new Intent(this, StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
