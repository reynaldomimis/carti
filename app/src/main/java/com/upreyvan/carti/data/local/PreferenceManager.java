package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.upreyvan.carti.util.Constants;

public class PreferenceManager {
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.Keys.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setUserData(String name, String email, String role, boolean isEmployed, String familyId, String inviteCode, String userId) {
        sharedPreferences.edit()
                .putString(Constants.Keys.KEY_USER_NAME, name)
                .putString(Constants.Keys.KEY_USER_ID_PREF, userId)
                .putString(Constants.Keys.KEY_USER_EMAIL, email)
                .putString(Constants.Keys.KEY_USER_ROLE, role)
                .putString(Constants.Keys.KEY_FAMILY_ID, familyId)
                .putString(Constants.Keys.KEY_INVITE_CODE, inviteCode)
                .putBoolean(Constants.Keys.KEY_IS_EMPLOYED, isEmployed)
                .apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(Constants.Keys.KEY_USER_ID_PREF, "");
    }

    public String getFamilyId() {
        return sharedPreferences.getString(Constants.Keys.KEY_FAMILY_ID, "");
    }

    public void setFamilyId(String familyId) {
        sharedPreferences.edit().putString(Constants.Keys.KEY_FAMILY_ID, familyId).apply();
    }

    public String getInviteCode() {
        return sharedPreferences.getString(Constants.Keys.KEY_INVITE_CODE, "");
    }

    public void setInviteCode(String inviteCode) {
        sharedPreferences.edit().putString(Constants.Keys.KEY_INVITE_CODE, inviteCode).apply();
    }

    public String getUserRole() {
        return sharedPreferences.getString(Constants.Keys.KEY_USER_ROLE, "Member");
    }

    public boolean isEmployed() {
        return sharedPreferences.getBoolean(Constants.Keys.KEY_IS_EMPLOYED, false);
    }

    public String getUserName() {
        return sharedPreferences.getString(Constants.Keys.KEY_USER_NAME, "User");
    }

    public String getUserEmail() {
        return sharedPreferences.getString(Constants.Keys.KEY_USER_EMAIL, "");
    }

    public void saveFamilySummary(double balance, double income, double expense) {
        sharedPreferences.edit()
                .putLong(Constants.Keys.KEY_BALANCE, Double.doubleToRawLongBits(balance))
                .putLong(Constants.Keys.KEY_TOTAL_INCOME, Double.doubleToRawLongBits(income))
                .putLong(Constants.Keys.KEY_TOTAL_EXPENSE, Double.doubleToRawLongBits(expense))
                .apply();
    }

    public double getBalance() {
        return Double.longBitsToDouble(sharedPreferences.getLong(Constants.Keys.KEY_BALANCE, 0));
    }

    public double getTotalIncome() {
        return Double.longBitsToDouble(sharedPreferences.getLong(Constants.Keys.KEY_TOTAL_INCOME, 0));
    }

    public double getTotalExpense() {
        return Double.longBitsToDouble(sharedPreferences.getLong(Constants.Keys.KEY_TOTAL_EXPENSE, 0));
    }

    public void setOnboardingFinished(boolean finished) {
        sharedPreferences.edit().putBoolean(Constants.Keys.KEY_ONBOARDING_FINISHED, finished).apply();
    }

    public boolean isOnboardingFinished() {
        return sharedPreferences.getBoolean(Constants.Keys.KEY_ONBOARDING_FINISHED, false);
    }

    public void setChatAutoDeleteDays(int days) {
        sharedPreferences.edit().putInt(Constants.Keys.KEY_CHAT_AUTO_DELETE_DAYS, days).apply();
    }

    public int getChatAutoDeleteDays() {
        return sharedPreferences.getInt(Constants.Keys.KEY_CHAT_AUTO_DELETE_DAYS, 7);
    }

    public void setLastSyncTime(String timestamp) {
        sharedPreferences.edit().putString(Constants.Keys.KEY_LAST_SYNC_TIME, timestamp).apply();
    }

    public String getLastSyncTime() {
        return sharedPreferences.getString(Constants.Keys.KEY_LAST_SYNC_TIME, "1970-01-01T00:00:00.000Z");
    }

    public void setLastGoalSyncTime(String timestamp) {
        sharedPreferences.edit().putString(Constants.Keys.KEY_LAST_GOAL_SYNC_TIME, timestamp).apply();
    }

    public String getLastGoalSyncTime() {
        return sharedPreferences.getString(Constants.Keys.KEY_LAST_GOAL_SYNC_TIME, "1970-01-01T00:00:00.000Z");
    }

    public void setLastDebtSyncTime(String timestamp) {
        sharedPreferences.edit().putString(Constants.Keys.KEY_LAST_DEBT_SYNC_TIME, timestamp).apply();
    }

    public String getLastDebtSyncTime() {
        return sharedPreferences.getString(Constants.Keys.KEY_LAST_DEBT_SYNC_TIME, "1970-01-01T00:00:00.000Z");
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
