package com.upreyvan.carti.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.AuthRepository;
import com.upreyvan.carti.databinding.ActivitySplashBinding;
import com.upreyvan.carti.ui.onboarding.StartActivity;
import com.upreyvan.carti.util.Constants;
import java.util.Map;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity<ActivitySplashBinding> {
    private boolean isNavigating = false;
    private AuthRepository authRepo;
    private PreferenceManager pref;

    @Override protected ActivitySplashBinding inflateBinding(LayoutInflater inflater) { return ActivitySplashBinding.inflate(inflater); }

    private final android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        if (!isNavigating) {
            android.util.Log.e("SplashActivity", "Startup timeout reached (15s). Forcing fallback navigation.");
            finalFallback();
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepo = AuthRepository.getInstance(this);
        pref = PreferenceManager.getInstance(this);
        
        getBinding().progressBar.setVisibility(View.VISIBLE);
        
        timeoutHandler.postDelayed(timeoutRunnable, 15000);
        validateSessionServerFirst();
    }

    private void validateSessionServerFirst() {
        authRepo.getCurrentUser(new AppwriteManager.AppwriteCallback<io.appwrite.models.User<Map<String, Object>>>() {
            @Override
            public void onSuccess(io.appwrite.models.User<Map<String, Object>> user) {
                fetchFullProfileAndRoute();
            }

            @Override
            public void onError(Throwable e) {
                cancelTimeout();
                navigateToLogin();
            }
        });
    }

    private void fetchFullProfileAndRoute() {
        authRepo.getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> r) {
                if (isNavigating) return;
                cancelTimeout();
                pref.saveUser(r);
                
                String familyId = (r.get("familyId") != null && !"null".equals(String.valueOf(r.get("familyId")))) ? String.valueOf(r.get("familyId")) : "";
                String pendingId = (r.get("pendingFamilyId") != null && !"null".equals(String.valueOf(r.get("pendingFamilyId")))) ? String.valueOf(r.get("pendingFamilyId")) : "";

                if (!familyId.isEmpty()) {
                    navigateToHome();
                } else if ("declined".equals(pendingId)) {
                    navigateToStatus(false, null, true);
                } else if (!pendingId.isEmpty()) {
                    navigateToStatus(true, null, false);
                } else {
                    navigateToOnboarding();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (isNavigating) return;
                String msg = e.getMessage();
                if (msg != null && (msg.contains("401") || msg.contains("Unauthorized") || msg.contains(Constants.ErrorCodes.UNAUTHORIZED))) {
                    pref.clear();
                    cancelTimeout();
                    navigateToLogin();
                } else {
                    finalFallback();
                }
            }
        });
    }

    private void finalFallback() {
        if (isNavigating) return;
        if (!pref.getFamilyId().isEmpty()) {
            navigateToHome();
        } else if (!pref.getUserId().isEmpty()) {
            navigateToOnboarding();
        } else {
            navigateToLogin();
        }
    }

    private void cancelTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }

    private void navigateToOnboarding() { if (isNavigating) return; isNavigating = true; cancelTimeout(); startActivity(new Intent(this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
    private void navigateToHome() { if (isNavigating) return; isNavigating = true; cancelTimeout(); startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
    private void navigateToLogin() { if (isNavigating) return; isNavigating = true; cancelTimeout(); startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
    
    private void navigateToStatus(boolean isWaiting, String inviteCode, boolean isDeclined) {
        if (isNavigating) return;
        isNavigating = true;
        cancelTimeout();
        Intent intent = new Intent(this, StartActivity.class);
        intent.putExtra("is_waiting", isWaiting);
        intent.putExtra("is_declined", isDeclined);
        if (inviteCode != null) intent.putExtra("invite_code", inviteCode);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override protected void onDestroy() {
        cancelTimeout();
        super.onDestroy();
    }
}
