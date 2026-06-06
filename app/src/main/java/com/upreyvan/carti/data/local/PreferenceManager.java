package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.SecurityManager;
import java.util.Map;

public class PreferenceManager {
    public static final String KEY_ADMIN_ID = "admin_id";

    private static volatile PreferenceManager instance;
    private SharedPreferences sharedPreferences;
    private final Context context;
    private final java.util.concurrent.CountDownLatch initLatch = new java.util.concurrent.CountDownLatch(1);

    private PreferenceManager(Context context) {
        this.context = context.getApplicationContext();
        new Thread(() -> {
            try {
                sharedPreferences = SecurityManager.getEncryptedPrefs(this.context, Constants.Keys.PREF_NAME);
            } finally {
                initLatch.countDown();
            }
        }).start();
    }

    public static PreferenceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PreferenceManager.class) {
                if (instance == null) {
                    instance = new PreferenceManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private SharedPreferences getPrefs() {
        try {
            initLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return sharedPreferences;
    }

    public void setUserData(String name, String email, String role, boolean isEmployed, String familyId, String inviteCode, String userId) {
        getPrefs().edit()
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
        String id = getPrefs().getString(Constants.Keys.KEY_USER_ID_PREF, "");
        return (id == null || id.equalsIgnoreCase("null")) ? "" : id.trim();
    }

    public void setUserId(String userId) {
        getPrefs().edit().putString(Constants.Keys.KEY_USER_ID_PREF, userId).commit();
    }

    public String getFamilyId() {
        String id = getPrefs().getString(Constants.Keys.KEY_FAMILY_ID, "");
        return (id == null || id.equalsIgnoreCase("null")) ? "" : id.trim();
    }

    public void setFamilyId(String familyId) {
        getPrefs().edit().putString(Constants.Keys.KEY_FAMILY_ID, familyId).commit();
    }

    public String getInviteCode() {
        return getPrefs().getString(Constants.Keys.KEY_INVITE_CODE, "");
    }

    public void setInviteCode(String inviteCode) {
        getPrefs().edit().putString(Constants.Keys.KEY_INVITE_CODE, inviteCode).apply();
    }

    public String getUserRole() {
        return getPrefs().getString(Constants.Keys.KEY_USER_ROLE, "Member");
    }

    public boolean isEmployed() {
        return getPrefs().getBoolean(Constants.Keys.KEY_IS_EMPLOYED, false);
    }

    public String getUsername() {
        return getPrefs().getString(Constants.Keys.KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return getPrefs().getString(Constants.Keys.KEY_USER_EMAIL, "");
    }

    public void saveFamilySummary(double balance, double income, double expense) {
        getPrefs().edit()
                .putLong(Constants.Keys.KEY_BALANCE, Double.doubleToRawLongBits(balance))
                .putLong(Constants.Keys.KEY_TOTAL_INCOME, Double.doubleToRawLongBits(income))
                .putLong(Constants.Keys.KEY_TOTAL_EXPENSE, Double.doubleToRawLongBits(expense))
                .apply();
    }

    public double getBalance() {
        return Double.longBitsToDouble(getPrefs().getLong(Constants.Keys.KEY_BALANCE, 0));
    }

    public double getTotalIncome() {
        return Double.longBitsToDouble(getPrefs().getLong(Constants.Keys.KEY_TOTAL_INCOME, 0));
    }

    public double getTotalExpense() {
        return Double.longBitsToDouble(getPrefs().getLong(Constants.Keys.KEY_TOTAL_EXPENSE, 0));
    }

    public void setOnboardingFinished(boolean finished) {
        getPrefs().edit().putBoolean(Constants.Keys.KEY_ONBOARDING_FINISHED, finished).apply();
    }

    public boolean isOnboardingFinished() {
        return getPrefs().getBoolean(Constants.Keys.KEY_ONBOARDING_FINISHED, false);
    }

    public void setChatAutoDeleteDays(int days) {
        getPrefs().edit().putInt(Constants.Keys.KEY_CHAT_AUTO_DELETE_DAYS, days).apply();
    }

    public int getChatAutoDeleteDays() {
        return getPrefs().getInt(Constants.Keys.KEY_CHAT_AUTO_DELETE_DAYS, 7);
    }

    public void setLastSyncTime(String timestamp) {
        getPrefs().edit().putString(Constants.Keys.KEY_LAST_SYNC_TIME, timestamp).apply();
    }

    public void setLastSyncTimeMillis(long millis) {
        getPrefs().edit().putLong("last_sync_millis", millis).apply();
    }

    public long getLastSyncTimeMillis() {
        return getPrefs().getLong("last_sync_millis", 0);
    }

    public Context getContext() {
        return context;
    }

    public void resetAllSyncTimestamps() {
        getPrefs().edit()
                .remove(Constants.Keys.KEY_LAST_SYNC_TIME)
                .apply();
    }

    public void resetLastSyncTime() {
        getPrefs().edit().remove(Constants.Keys.KEY_LAST_SYNC_TIME).apply();
    }

    public String getLastSyncTime() {
        return getPrefs().getString(Constants.Keys.KEY_LAST_SYNC_TIME, "1970-01-01T00:00:00.000Z");
    }

    public void setBudgetPlanDismissedMonth(String month) {
        getPrefs().edit().putString(Constants.Keys.KEY_BUDGET_PLAN_DISMISSED_MONTH, month).apply();
    }

    public String getBudgetPlanDismissedMonth() {
        return getPrefs().getString(Constants.Keys.KEY_BUDGET_PLAN_DISMISSED_MONTH, "");
    }

    public void setHasNotifications(boolean has) {
        getPrefs().edit().putBoolean(Constants.Keys.KEY_HAS_NOTIFICATIONS, has).apply();
    }

    public boolean hasNotifications() {
        return getPrefs().getBoolean(Constants.Keys.KEY_HAS_NOTIFICATIONS, false);
    }

    public void setAdminId(String adminId) {
        getPrefs().edit().putString(KEY_ADMIN_ID, adminId).apply();
    }

    public String getAdminId() {
        return getPrefs().getString(KEY_ADMIN_ID, "");
    }

    public boolean isAdmin() {
        String adminId = getAdminId();
        String currentUserId = getUserId();


        if (!adminId.isEmpty() && !"null".equals(adminId)) {
            if (currentUserId.equalsIgnoreCase(adminId.trim())) return true;
        }

       String role = getUserRole();
        if (role != null && !"null".equals(role)) {
            for (String r : Constants.Roles.PARENTS) {
                if (r.equalsIgnoreCase(role.trim())) return true;
            }
        }
        return false;
    }

    public void setLastNotifCheck(String timestamp) {
        getPrefs().edit().putString(Constants.Keys.KEY_LAST_NOTIF_CHECK, timestamp).apply();
    }

    public String getLastNotifCheck() {
        return getPrefs().getString(Constants.Keys.KEY_LAST_NOTIF_CHECK, "1970-01-01T00:00:00.000Z");
    }

    public void setLastRecurringCheck(String yearMonth) {
        getPrefs().edit().putString("last_recurring_check", yearMonth).apply();
    }

    public String getLastRecurringCheck() {
        return getPrefs().getString("last_recurring_check", "");
    }

    public void setAiIntroDone(boolean done) { getPrefs().edit().putBoolean("ai_intro_done", done).apply(); }
    public boolean isAiIntroDone() { return getPrefs().getBoolean("ai_intro_done", false); }

    public void saveUser(Map<String, Object> data) {
        if (data == null) return;
        
        // Handle both Appwrite standard '$id' and Cloud Function custom 'userId'
        Object idObj = data.get("userId");
        if (idObj == null) idObj = data.get("$id");
        String finalId = String.valueOf(idObj);

        setUserData(
                String.valueOf(data.get("username")),
                String.valueOf(data.get("email")),
                String.valueOf(data.get("role")),
                Boolean.TRUE.equals(data.get("isEmployed")),
                String.valueOf(data.get("familyId")),
                String.valueOf(data.get("inviteCode")),
                finalId
        );
        String pendingId = String.valueOf(data.get("pendingFamilyId"));
        getPrefs().edit().putString("pending_family_id", (pendingId == null || "null".equals(pendingId)) ? "" : pendingId).apply();
    }

    public String getPendingFamilyId() {
        String id = getPrefs().getString("pending_family_id", "");
        return (id == null || id.equalsIgnoreCase("null")) ? "" : id.trim();
    }

    public void clear() {
        String[] prefFiles = {
                Constants.Keys.PREF_NAME,
                "pref_budget_plan",
                "io.appwrite.auth",
                Constants.Keys.PREF_EXPENSE,
                Constants.Keys.PREF_GOAL,
                Constants.Keys.PREF_SALARY,
                Constants.Keys.PREF_CATEGORY,
                Constants.Keys.PREF_DEBT
        };

        for (String fileName : prefFiles) {
            try {
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit().clear().apply();
            } catch (Exception e) {
                android.util.Log.e("PreferenceManager", "Error clearing " + fileName, e);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    context.deleteSharedPreferences(fileName);
                } catch (Exception e) {
                    android.util.Log.e("PreferenceManager", "Error deleting " + fileName, e);
                }
            }
        }
        sharedPreferences = context.getSharedPreferences(Constants.Keys.PREF_NAME, Context.MODE_PRIVATE);
    }
}
