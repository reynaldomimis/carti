package com.upreyvan.carti.data.remote;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.data.local.PreferenceManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.appwrite.Query;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Execution;

/**
 * Senior Developer Refactored: Optimized for Production.
 * - SDK for READS: Saves costs/bandwidth by fetching directly from DB with Delta Sync.
 * - Functions for WRITES: Maintains security by processing logic on the server.
 */
public class ApiHelper {
    private final AppwriteManager appwriteManager;
    private final PreferenceManager pref;
    private final Gson gson = new Gson();

    // DATABASE CONFIGURATION (Ensure these match your Appwrite console)
    private static final String DATABASE_ID = "69eca97100090be1e45e"; // Actual DB ID
    private static final String COL_USERS = "users";
    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_GOALS = "goals";
    private static final String COL_DEBTS = "debts";
    private static final String COL_FAMILIES = "families";

    public ApiHelper(Context context) {
        this.appwriteManager = AppwriteManager.getInstance(context);
        this.pref = new PreferenceManager(context);
    }

    /**
     * Unified method to call any action on the gateway function (WRITES).
     */
    private void callAction(String action, Map<String, Object> params, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        appwriteManager.callGateway(action, params, new AppwriteManager.AppwriteCallback<Execution>() {
            @Override
            public void onSuccess(Execution result) {
                try {
                    Type type = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> response = gson.fromJson(result.getResponseBody(), type);
                    
                    if (response != null && Boolean.TRUE.equals(response.get("s"))) {
                        Object data = response.get("data");
                        if (data instanceof Map) {
                            callback.onSuccess((Map<String, Object>) data);
                        } else if (data instanceof java.util.List) {
                            Map<String, Object> wrapper = new HashMap<>();
                            wrapper.put("list", data);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onSuccess(new HashMap<>());
                        }
                    } else {
                        String message = response != null && response.get("m") != null ? String.valueOf(response.get("m")) : "Unknown server error";
                        callback.onError(new Exception(message));
                    }
                } catch (Exception e) {
                    callback.onError(new Exception("Failed to parse server response: " + e.getMessage()));
                }
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void sync(String lastSyncTime, String startDate, String endDate, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("lastSyncTime", lastSyncTime);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        callAction("sync", params, callback);
    }

    /* ─────────────────────────────────────────────────────────────
       SDK READ OPERATIONS (COST OPTIMIZED)
    ───────────────────────────────────────────────────────────── */

    public void getTransactions(String startDate, String endDate, String lastSyncTime, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception("NO_FAMILY")); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId)); // Security
        queries.add(Query.Companion.greaterThanEqual("$createdAt", startDate));
        queries.add(Query.Companion.lessThanEqual("$createdAt", endDate));
        queries.add(Query.Companion.orderDesc("$createdAt"));
        queries.add(Query.Companion.limit(100));

        if (lastSyncTime != null && !lastSyncTime.isEmpty()) {
            queries.add(Query.Companion.greaterThan("$updatedAt", lastSyncTime));
        }
        appwriteManager.listDocuments(DATABASE_ID, COL_TRANSACTIONS, queries, callback);
    }

    public void getMembers(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        appwriteManager.listDocuments(DATABASE_ID, COL_USERS, queries, callback);
    }

    public void getGoals(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        appwriteManager.listDocuments(DATABASE_ID, COL_GOALS, queries, callback);
    }

    public void getDebts(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        appwriteManager.listDocuments(DATABASE_ID, COL_DEBTS, queries, callback);
    }

    public void getFamilySummary(AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        appwriteManager.getDocument(DATABASE_ID, COL_FAMILIES, familyId, callback);
    }

    /* ─────────────────────────────────────────────────────────────
       FUNCTION WRITE OPERATIONS (SECURE GATEWAY)
    ───────────────────────────────────────────────────────────── */

    public void register(String email, String password, String username, boolean isEmployed, String role, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);
        params.put("username", username);
        params.put("isEmployed", isEmployed);
        params.put("role", role);
        callAction("register", params, callback);
    }

    public void getUser(AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        callAction("get_user", new HashMap<>(), callback);
    }

    public void createFamily(String familyName, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("familyName", familyName);
        callAction("create_family", params, callback);
    }

    public void joinFamily(String inviteCode, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("inviteCode", inviteCode);
        callAction("join_family", params, callback);
    }

    public void addTransaction(double amount, String type, String category, String note, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("type", type);
        params.put("category", category);
        params.put("note", note);
        callAction("add_transaction", params, callback);
    }

    public void addGoal(String name, double targetAmount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("targetAmount", targetAmount);
        callAction("add_goal", params, callback);
    }

    public void addDebt(String personName, double amount, String type, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("personName", personName);
        params.put("amount", amount);
        params.put("type", type);
        callAction("add_debt", params, callback);
    }

    public void markDebtPaid(String debtId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("debtId", debtId);
        callAction("mark_debt_paid", params, callback);
    }

    public void deleteTransaction(String transactionId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("transactionId", transactionId);
        callAction("delete_transaction", params, callback);
    }
}
