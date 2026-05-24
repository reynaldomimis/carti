package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.upreyvan.carti.util.Constants;

public class PreferenceManager {
    public static final String KEY_ADMIN_ID = "admin_id";

    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.Keys.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static PreferenceManager getInstance(Context context) {
        return new PreferenceManager(context);
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
        String id = sharedPreferences.getString(Constants.Keys.KEY_USER_ID_PREF, "");
        return (id == null || id.equalsIgnoreCase("null")) ? "" : id.trim();
    }

    public String getFamilyId() {
        String id = sharedPreferences.getString(Constants.Keys.KEY_FAMILY_ID, "");
        return (id == null || id.equals("null")) ? "" : id;
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

    public String getUsername() {
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

    public void resetAllSyncTimestamps() {
        sharedPreferences.edit()
                .remove(Constants.Keys.KEY_LAST_SYNC_TIME)
                .remove(Constants.Keys.KEY_LAST_GOAL_SYNC_TIME)
                .remove(Constants.Keys.KEY_LAST_DEBT_SYNC_TIME)
                .apply();
    }

    public void resetLastSyncTime() {
        sharedPreferences.edit().remove(Constants.Keys.KEY_LAST_SYNC_TIME).apply();
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

    public void setBudgetPlanDismissedMonth(String month) {
        sharedPreferences.edit().putString(Constants.Keys.KEY_BUDGET_PLAN_DISMISSED_MONTH, month).apply();
    }

    public String getBudgetPlanDismissedMonth() {
        return sharedPreferences.getString(Constants.Keys.KEY_BUDGET_PLAN_DISMISSED_MONTH, "");
    }

    public void setHasNotifications(boolean has) {
        sharedPreferences.edit().putBoolean(Constants.Keys.KEY_HAS_NOTIFICATIONS, has).apply();
    }

    public boolean hasNotifications() {
        return sharedPreferences.getBoolean(Constants.Keys.KEY_HAS_NOTIFICATIONS, false);
    }

    public void setAdminId(String adminId) {
        sharedPreferences.edit().putString(KEY_ADMIN_ID, adminId).apply();
    }

    public String getAdminId() {
        return sharedPreferences.getString(KEY_ADMIN_ID, "");
    }

    public boolean isAdmin() {
        String adminId = getAdminId();
        String currentUserId = getUserId();

        // 1. Check if user is the designated family admin
        if (!adminId.isEmpty() && !"null".equals(adminId)) {
            if (currentUserId.equalsIgnoreCase(adminId.trim())) return true;
        }

        // 2. Always fallback to checking roles (Parents are always admins)
        String role = getUserRole();
        if (role != null && !"null".equals(role)) {
            for (String r : Constants.Roles.PARENTS) {
                if (r.equalsIgnoreCase(role.trim())) return true;
            }
        }
        return false;
    }

    public void setLastNotifCheck(String timestamp) {
        sharedPreferences.edit().putString(Constants.Keys.KEY_LAST_NOTIF_CHECK, timestamp).apply();
    }

    public String getLastNotifCheck() {
        return sharedPreferences.getString(Constants.Keys.KEY_LAST_NOTIF_CHECK, "1970-01-01T00:00:00.000Z");
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
