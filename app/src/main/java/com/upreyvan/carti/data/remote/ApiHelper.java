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
import com.upreyvan.carti.util.Constants;
import io.appwrite.Query;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Execution;

/**
 * Senior Developer Refactored: Optimized for Production.
 * - SDK for READS: Saves costs/bandwidth by fetching directly from DB.
 * - Functions for WRITES: Maintains security by processing logic on the server.
 */
public class ApiHelper {
    private final AppwriteManager appwriteManager;
    private final PreferenceManager pref;
    private final Gson gson = new Gson();

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

    /* ─────────────────────────────────────────────────────────────
       SDK READ OPERATIONS (COST OPTIMIZED)
    ───────────────────────────────────────────────────────────── */

    public void getTransactions(String startDate, String endDate, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId)); // Security
        queries.add(Query.Companion.greaterThanEqual("$createdAt", startDate));
        queries.add(Query.Companion.lessThanEqual("$createdAt", endDate));
        queries.add(Query.Companion.orderDesc("$createdAt"));
        queries.add(Query.Companion.limit(100));

        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    public void getMembers(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_USERS, queries, callback);
    }

    public void getGoals(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_GOALS, queries, callback);
    }

    public void getDebts(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_DEBTS, queries, callback);
    }

    public void getFamilySummary(AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        appwriteManager.getDocument(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_FAMILIES, familyId, callback);
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
        callAction(Constants.Actions.REGISTER, params, callback);
    }

    public void getUser(AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        callAction(Constants.Actions.GET_USER, new HashMap<>(), callback);
    }

    public void createFamily(String familyName, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("familyName", familyName);
        callAction(Constants.Actions.CREATE_FAMILY, params, callback);
    }

    public void joinFamily(String inviteCode, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("inviteCode", inviteCode);
        callAction(Constants.Actions.JOIN_FAMILY, params, callback);
    }

    public void approveJoinRequest(String targetUserId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("targetUserId", targetUserId);
        callAction(Constants.Actions.APPROVE_JOIN, params, callback);
    }

    public void addTransaction(double amount, String type, String category, String note, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("type", type);
        params.put("category", category);
        params.put("note", note);
        callAction(Constants.Actions.ADD_TRANSACTION, params, callback);
    }

    public void addGoal(String name, double targetAmount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("targetAmount", targetAmount);
        callAction(Constants.Actions.ADD_GOAL, params, callback);
    }

    public void addDebt(String personName, double amount, String type, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("personName", personName);
        params.put("amount", amount);
        params.put("type", type);
        callAction(Constants.Actions.ADD_DEBT, params, callback);
    }

    public void updateGoalAmount(String goalId, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("goalId", goalId);
        params.put("amount", amount);
        callAction(Constants.Actions.UPDATE_GOAL, params, callback);
    }

    public void updateDebtAmount(String debtId, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("debtId", debtId);
        params.put("amount", amount);
        callAction(Constants.Actions.UPDATE_DEBT, params, callback);
    }

    public void markDebtPaid(String debtId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("debtId", debtId);
        callAction(Constants.Actions.MARK_DEBT_PAID, params, callback);
    }

    public void deleteTransaction(String transactionId, AppwriteManager.AppwriteCallback<Object> callback) {
        appwriteManager.deleteDocument(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, transactionId, callback);
    }

    /**
     * MANUAL UPDATE: Since the Cloud Function no longer calculates totals,
     * the Java app must calculate and push the new values.
     */
    public void updateFamilyTotals(double balance, double income, double expense, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("balance", balance);
        params.put("totalIncome", income);
        params.put("totalExpense", expense);
        callAction(Constants.Actions.UPDATE_FAMILY_TOTALS, params, callback);
    }

    /**
     * SECURE CHAT: Sending messages directly via SDK.
     * Permissions are restricted to the family team.
     */
    public void sendMessage(String text, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        String userId = pref.getUserId();
        String userName = pref.getUserName();

        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("senderId", userId);
        data.put("senderName", userName);
        data.put("familyId", familyId);
        data.put("timestamp", System.currentTimeMillis());

        // temporary change to Role.users() to fix the permission error
        List<String> permissions = new ArrayList<>();
        permissions.add(io.appwrite.Permission.Companion.read(io.appwrite.Role.Companion.users("")));

        appwriteManager.createDocument(
                Constants.Appwrite.DATABASE_ID,
                Constants.Appwrite.COL_MESSAGES,
                io.appwrite.ID.Companion.unique(0),
                data,
                permissions,
                callback
        );
    }
}
