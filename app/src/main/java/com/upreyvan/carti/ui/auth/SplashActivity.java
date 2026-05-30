package com.upreyvan.carti.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
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
        PreferenceManager pref = new PreferenceManager(this);
        ApiHelper apiHelper = new ApiHelper(this);

        // SENIOR STRATEGY: 
        // 1. If we don't even have a locally saved userId, go straight to Login.
        // This stops the "looping" feel for new/logged-out users.
        if (pref.getUserId().isEmpty()) {
            navigateToLogin();
            return;
        }

        apiHelper.getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> userDoc) {
                // Harmonize keys with AuthFragment
                String userId = String.valueOf(userDoc.getOrDefault("$id", ""));
                String name = String.valueOf(userDoc.getOrDefault("username", userDoc.getOrDefault("name", "User")));
                String email = String.valueOf(userDoc.getOrDefault("email", ""));
                String role = String.valueOf(userDoc.getOrDefault("role", ""));
                
                boolean isEmployed = false;
                Object emp = userDoc.get("isEmployed");
                if (emp instanceof Boolean) isEmployed = (Boolean) emp;
                else if (emp != null) isEmployed = Boolean.parseBoolean(String.valueOf(emp));
                
                String familyId = (userDoc.get("familyId") != null && !"null".equals(String.valueOf(userDoc.get("familyId")))) ? String.valueOf(userDoc.get("familyId")) : "";
                String inviteCode = (userDoc.get("inviteCode") != null && !"null".equals(String.valueOf(userDoc.get("inviteCode")))) ? String.valueOf(userDoc.get("inviteCode")) : "";

                // Update preferences
                pref.setUserData(name, email, role, isEmployed, familyId, inviteCode, userId);

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
                String message = error.getMessage();
                boolean isUnauthorized = message != null && (
                        message.contains("401") || 
                        message.contains("Unauthorized") || 
                        message.contains("login") || 
                        message.contains("session")
                );

                if (isUnauthorized) {
                    new Thread(() -> {
                        try {
                            AppDatabase.getInstance(SplashActivity.this).clearAllTables();
                        } catch (Exception ignored) {}
                        runOnUiThread(() -> {
                            pref.clear();
                            navigateToLogin();
                        });
                    }).start();
                } else {
                    // Network error or other - if we have a familyId, allow offline entry to Home
                    if (!pref.getFamilyId().isEmpty()) {
                        navigateToHome();
                    } else {
                        navigateToLogin();
                    }
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
