package com.upreyvan.carti.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.databinding.ActivitySplashBinding;
import com.upreyvan.carti.ui.onboarding.StartActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity<ActivitySplashBinding> {
    private boolean isNavigating = false;

    @Override protected ActivitySplashBinding inflateBinding(LayoutInflater inflater) { return ActivitySplashBinding.inflate(inflater); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {
            if (isNavigating) return;
            PreferenceManager pref = new PreferenceManager(this);
            
            if (pref.getUserId().isEmpty()) {
                navigateToLogin();
            } else {
                if (pref.getFamilyId().isEmpty()) navigateToOnboarding();
                else navigateToHome();
            }
        }, 1500);
    }

    private void navigateToOnboarding() { if (isNavigating) return; isNavigating = true; startActivity(new Intent(this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
    private void navigateToHome() { if (isNavigating) return; isNavigating = true; startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
    private void navigateToLogin() { if (isNavigating) return; isNavigating = true; startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
}
