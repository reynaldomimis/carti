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
    private static final String KEY_INVITE_CODE = "invite_code";
    private static final String KEY_IS_EMPLOYED = "is_employed";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setUserData(String name, String email, String role, boolean isEmployed, String familyId, String inviteCode) {
        sharedPreferences.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_FAMILY_ID, familyId)
                .putString(KEY_INVITE_CODE, inviteCode)
                .putBoolean(KEY_IS_EMPLOYED, isEmployed)
                .apply();
    }

    public String getLastSyncTime() {
        return sharedPreferences.getString(KEY_LAST_SYNC_TIME, "");
    }

    public void setLastSyncTime(String time) {
        sharedPreferences.edit().putString(KEY_LAST_SYNC_TIME, time).apply();
    }

    public String getFamilyId() {
        return sharedPreferences.getString(KEY_FAMILY_ID, "");
    }

    public void setFamilyId(String familyId) {
        sharedPreferences.edit().putString(KEY_FAMILY_ID, familyId).apply();
    }

    public String getInviteCode() {
        return sharedPreferences.getString(KEY_INVITE_CODE, "");
    }

    public void setInviteCode(String inviteCode) {
        sharedPreferences.edit().putString(KEY_INVITE_CODE, inviteCode).apply();
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
