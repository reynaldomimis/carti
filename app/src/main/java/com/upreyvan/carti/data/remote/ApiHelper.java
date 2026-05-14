package com.upreyvan.carti.data.remote;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import io.appwrite.models.Execution;

/**
 * Helper class to interact with Appwrite Functions (Gateway).
 */
public class ApiHelper {
    private final AppwriteManager appwriteManager;
    private final Gson gson = new Gson();

    public ApiHelper(Context context) {
        this.appwriteManager = AppwriteManager.getInstance(context);
    }

    /**
     * Register a new user using the Appwrite Function.
     */
    public void register(String email, String password, String username, boolean isEmployed, String role, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);
        params.put("username", username);
        params.put("isEmployed", isEmployed);
        params.put("role", role);
        callAction("register", params, callback);
    }

    /**
     * Create a new family group.
     */
    public void createFamily(String familyName, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", familyName);
        callAction("create_family", params, callback);
    }

    /**
     * Join an existing family using an invite code.
     */
    public void joinFamily(String inviteCode, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("inviteCode", inviteCode);
        callAction("join_family", params, callback);
    }

    /**
     * Sync data from the server.
     */
    public void sync(String lastSyncTime, String startDate, String endDate, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("lastSyncTime", lastSyncTime);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        callAction("sync", params, callback);
    }

    /**
     * Add a new transaction (Income/Expense).
     */
    public void addTransaction(double amount, String type, String category, String note, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("type", type);
        params.put("category", category);
        params.put("note", note);
        callAction("add_transaction", params, callback);
    }

    /**
     * Delete a transaction.
     */
    public void deleteTransaction(String docId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("docId", docId);
        callAction("delete_transaction", params, callback);
    }

    /**
     * Add a new goal.
     */
    public void addGoal(String name, double targetAmount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("targetAmount", targetAmount);
        callAction("add_goal", params, callback);
    }

    /**
     * Add a new debt.
     */
    public void addDebt(String personName, double amount, String type, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("personName", personName);
        params.put("amount", amount);
        params.put("type", type);
        callAction("add_debt", params, callback);
    }

    /**
     * Get the full user document/context.
     */
    public void getUser(AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        callAction("get_user", new HashMap<>(), callback);
    }

    /**
     * Generic method to call any action on the gateway function.
     */
    public void callAction(String action, Map<String, Object> params, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
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
                        } else {
                            callback.onSuccess(new HashMap<>());
                        }
                    } else {
                        String message = "Unknown error from server";
                        if (response != null) {
                            if (response.get("m") != null) {
                                message = String.valueOf(response.get("m"));
                            } else if (response.get("message") != null) {
                                message = String.valueOf(response.get("message"));
                            }
                        }
                        
                        // User-friendly mapping for common errors
                        if (message.contains("already exists")) {
                            message = "Account already exists. Please login instead.";
                        }

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
}
