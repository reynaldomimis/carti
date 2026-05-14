package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "carti_prefs";
    private static final String KEY_ONBOARDING_FINISHED = "onboarding_finished";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_FAMILY_ID = "family_id";
    private static final String KEY_IS_EMPLOYED = "is_employed";
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setUserData(String name, String email, String role, boolean isEmployed, String familyId) {
        sharedPreferences.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_FAMILY_ID, familyId)
                .putBoolean(KEY_IS_EMPLOYED, isEmployed)
                .apply();
    }

    public String getFamilyId() {
        return sharedPreferences.getString(KEY_FAMILY_ID, "");
    }

    public void setFamilyId(String familyId) {
        sharedPreferences.edit().putString(KEY_FAMILY_ID, familyId).apply();
    }

    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, "Member");
    }

    public boolean isEmployed() {
        return sharedPreferences.getBoolean(KEY_IS_EMPLOYED, false);
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "User");
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, "");
    }

    public void setOnboardingFinished(boolean finished) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_FINISHED, finished).apply();
    }

    public boolean isOnboardingFinished() {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_FINISHED, false);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
