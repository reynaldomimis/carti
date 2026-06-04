package com.upreyvan.carti.data.remote.source;

import android.content.Context;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.util.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.appwrite.Query;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Execution;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TransactionRemoteDataSource {
    private final AppwriteManager appwriteManager;
    private final PreferenceManager pref;
    private final Gson gson = new Gson();

    public TransactionRemoteDataSource(Context context) {
        this.appwriteManager = AppwriteManager.getInstance(context);
        this.pref = PreferenceManager.getInstance(context);
    }

    public void addTransaction(double amount, String type, String category, String description, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("type", type);
        params.put("category", category);
        params.put("description", description);
        appwriteManager.callGateway(Constants.Actions.ADD_TRANSACTION, params, wrapExecution(callback));
    }

    public void addGoal(String name, double targetAmount, String dueDate, List<String> members, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", name);
        params.put("targetAmount", targetAmount);
        params.put("dueDate", dueDate);
        if (members != null) params.put("members", members);
        params.put("type", "GOAL");
        appwriteManager.callGateway(Constants.Actions.ADD_GOAL, params, wrapExecution(callback));
    }

    public void getTransactionsSince(String lastSync, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) {
            callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY));
            return;
        }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        if (lastSync != null && !lastSync.isEmpty()) {
            queries.add(Query.Companion.greaterThan("$updatedAt", lastSync));
        }
        queries.add(Query.Companion.orderDesc("$updatedAt"));
        queries.add(Query.Companion.limit(100)); 
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    public void updateTransaction(String id, Map<String, Object> data, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>(data);
        params.put("id", id);
        appwriteManager.callGateway(Constants.Actions.UPDATE_TRANSACTION, params, wrapExecution(callback));
    }

    public void deleteTransaction(String id, AppwriteManager.AppwriteCallback<Object> callback) {
        appwriteManager.deleteDocument(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, id, callback);
    }

    public void addLike(String transactionId, String emoji, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("transactionId", transactionId);
        params.put("emojiType", emoji);
        appwriteManager.callGateway(Constants.Actions.ADD_LIKE, params, wrapExecution(callback));
    }

    public void removeLike(String likeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("likeId", likeId);
        appwriteManager.callGateway(Constants.Actions.REMOVE_LIKE, params, wrapExecution(callback));
    }

    public void addComment(String transactionId, String text, String parentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("transactionId", transactionId);
        params.put("text", text);
        if (parentId != null) params.put("parentId", parentId);
        appwriteManager.callGateway(Constants.Actions.ADD_COMMENT, params, wrapExecution(callback));
    }

    public void deleteComment(String commentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("commentId", commentId);
        appwriteManager.callGateway(Constants.Actions.DELETE_COMMENT, params, wrapExecution(callback));
    }

    public void getComments(String transactionId, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("transactionId", transactionId));
        queries.add(Query.Companion.orderAsc("$createdAt"));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_COMMENTS, queries, callback);
    }

    @SuppressWarnings("unchecked")
    private AppwriteManager.AppwriteCallback<Execution> wrapExecution(AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        return new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Execution result) {
                try {
                    String responseBody = result.getResponseBody();
                    Map<String, Object> response = gson.fromJson(responseBody, new TypeToken<Map<String, Object>>(){}.getType());
                    if (response != null && Objects.equals(response.get("success"), true)) {
                        callback.onSuccess((Map<String, Object>) response.get("data"));
                    } else {
                        callback.onError(new Exception(response != null ? (String) response.get("message") : "Operation failed"));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        };
    }
}
