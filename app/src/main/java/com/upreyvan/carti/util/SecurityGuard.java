package com.upreyvan.carti.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.BuildConfig;
import java.security.MessageDigest;
import java.util.Locale;

public class SecurityGuard {

    private static final String EXPECTED_HASH = BuildConfig.APP_SIGNATURE_HASH;

    // Known harmful package names (Lucky Patcher, GameGuardian, APK Editors, Magisk, etc.)
    private static final String[] ILLEGAL_PACKAGES = {
            "com.chelpus.lackypatch",
            "com.dimonvideo.luckypatcher",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "com.android.vending.billing.InAppBillingService.CLONE",
            "com.android.vending.billing.InAppBillingService.LOCK",
            "com.android.vending.billing.InAppBillingService.COIN",
            "org.adblockplus.android",
            "com.apkeditor.pro",
            "com.apkeditor.premium",
            "com.apkeditor.free",
            "com.gmail.heagoo.apkeditor",
            "com.gmail.heagoo.apkeditor.pro",
            "catch_.me_.if_.you_.can_",
            "com.topjohnwu.magisk"
    };

    public static void checkIntegrity(Context context) {
        // 1. Signature Check
        if (!isValidSignature(context)) {
            Log.e("SecurityGuard", "TAMPERING DETECTED: App signature mismatch.");
            throw new RuntimeException("Security violation: App integrity compromised.");
        }

        // 2. Illegal Apps Check
        String detectedApp = getDetectedIllegalApp(context);
        if (detectedApp != null) {
            showSecurityViolationDialog(context, "Security Threat Detected", 
                "Your device has a restricted application installed: " + detectedApp + 
                "\n\nFor security reasons, Carti cannot run. Please uninstall it to continue.");
        }
    }

    private static boolean isValidSignature(Context context) {
        if (EXPECTED_HASH == null || EXPECTED_HASH.isEmpty() || EXPECTED_HASH.equals("null")) {
            Log.w("SecurityGuard", "No signature hash defined. Skipping check for development.");
            return true;
        }

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            
            if (packageInfo.signatures == null) return false;

            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();
                
                StringBuilder hexString = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    String hex = Integer.toHexString(0xff & digest[i]).toUpperCase(Locale.US);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                    if (i < digest.length - 1) hexString.append(':');
                }
                
                String currentHash = hexString.toString();
                
                // Log.d("SecurityGuard", "ACTUAL: " + currentHash);
                
                if (EXPECTED_HASH.equals(currentHash)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("SecurityGuard", "Error checking signature", e);
        }
        return false;
    }

    private static String getDetectedIllegalApp(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String pkg : ILLEGAL_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    private static void showSecurityViolationDialog(Context context, String title, String message) {
        if (!(context instanceof Activity)) return;
        Activity activity = (Activity) context;

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Exit", (dialog, which) -> {
                    activity.finishAffinity();
                    System.exit(0);
                })
                .show();
    }
}
