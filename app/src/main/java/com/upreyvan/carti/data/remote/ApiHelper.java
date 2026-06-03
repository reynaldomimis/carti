package com.upreyvan.carti.data.remote;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.data.local.PreferenceManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;
import io.appwrite.Query;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Execution;

public class ApiHelper {
    private final AppwriteManager appwriteManager;
    private final PreferenceManager pref;
    private final Gson gson = new Gson();
    private final Context context;

    public ApiHelper(Context context) {
        this.appwriteManager = AppwriteManager.getInstance(context);
        this.pref = PreferenceManager.getInstance(context);
        this.context = context.getApplicationContext();
    }

    public Context getContext() { return context; }

    public void callAction(String action, Map<String, Object> params, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        appwriteManager.callGateway(action, params, new AppwriteManager.AppwriteCallback<Execution>() {
            @Override
            public void onSuccess(Execution result) {
                try {
                    String responseBody = result.getResponseBody();
                    android.util.Log.d("ApiHelper", "Response [" + action + "]: " + responseBody);

                    Type type = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> response = gson.fromJson(responseBody, type);

                    boolean isSuccess = response != null && Boolean.TRUE.equals(response.get("success"));

                    if (isSuccess) {
                        Object data = response.get("data");
                        if (data instanceof Map) {
                            callback.onSuccess((Map<String, Object>) data);
                        } else if (data instanceof List) {
                            Map<String, Object> wrapper = new HashMap<>();
                            wrapper.put("list", data);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onSuccess(new HashMap<>());
                        }
                    } else {
                        String errorMsg = Constants.ErrorCodes.GENERIC_ERROR;
                        if (response != null && response.containsKey("error")) {
                            Object errorObj = response.get("error");
                            if (errorObj instanceof Map) {
                                Object msg = ((Map<?, ?>) errorObj).get("message");
                                if (msg != null) errorMsg = String.valueOf(msg);
                            }
                        }
                        callback.onError(new Exception(errorMsg));
                    }
                } catch (Exception e) {
                    android.util.Log.e("ApiHelper", "Parse Error", e);
                    callback.onError(new Exception(Constants.ErrorCodes.PARSE_ERROR));
                }
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    // ─── AUTH ────────────────────────────────────────────────────────────────
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

    // ─── FAMILY ──────────────────────────────────────────────────────────────

    public void createFamily(String familyName, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", familyName);
        callAction(Constants.Actions.CREATE_FAMILY, params, callback);
    }

    public void joinFamily(String inviteCode, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("inviteCode", inviteCode.toUpperCase()); // Para laging uppercase
        callAction(Constants.Actions.JOIN_FAMILY, params, callback);
    }

    public void approveJoinRequest(String applicantId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("applicantId", applicantId);
        params.put("accept", true);
        callAction(Constants.Actions.APPROVE_JOIN, params, callback);
    }

    public void rejectJoinRequest(String applicantId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("applicantId", applicantId);
        params.put("accept", false);
        callAction(Constants.Actions.APPROVE_JOIN, params, callback);
    }

    public void leaveFamily(AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        callAction(Constants.Actions.LEAVE_FAMILY, new HashMap<>(), callback);
    }

    public void deleteAccount(AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        callAction(Constants.Actions.DELETE_ACCOUNT, new HashMap<>(), callback);
    }

    // ─── TRANSACTIONS ────────────────────────────────────────────────────────
    public void addTransaction(double amount, String type, String category, String note, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", pref.getUsername());
        params.put("amount", amount);
        params.put("type", type);
        params.put("category", category);
        params.put("note", note);
        params.put("startDate", Utils.getCurrentTimestamp());
        callAction(Constants.Actions.ADD_TRANSACTION, params, callback);
    }

    public void updateTransaction(String txnId, Map<String, Object> fields, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>(fields);
        params.put("txnId", txnId);
        callAction(Constants.Actions.UPDATE_TRANSACTION, params, callback);
    }

    public void deleteTransaction(String transactionId, AppwriteManager.AppwriteCallback<Object> callback) {
        appwriteManager.deleteDocument(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, transactionId, callback);
    }

    public void getTransactionsSince(String sinceTimestamp, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        queries.add(Query.Companion.greaterThan("$createdAt", sinceTimestamp));
        queries.add(Query.Companion.orderDesc("$createdAt"));
        queries.add(Query.Companion.limit(1000));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    public void getTransactionsRange(String startDate, String endDate, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        queries.add(Query.Companion.between("$createdAt", startDate, endDate));
        queries.add(Query.Companion.orderDesc("$createdAt"));
        queries.add(Query.Companion.limit(1000));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    // ─── SOCIAL INTERACTIONS ─────────────────────────────────────────────────
    public void addLike(String transactionId, String emojiType, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("transactionId", transactionId);
        params.put("emojiType", emojiType);
        callAction(Constants.Actions.ADD_LIKE, params, callback);
    }

    public void removeLike(String likeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("likeId", likeId);
        callAction(Constants.Actions.REMOVE_LIKE, params, callback);
    }

    public void addComment(String transactionId, String text, String parentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("transactionId", transactionId);
        params.put("text", text);
        if (parentId != null) {
            params.put(Constants.Keys.KEY_PARENT_ID, parentId);
        }
        callAction(Constants.Actions.ADD_COMMENT, params, callback);
    }

    public void updateComment(String commentId, String text, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("commentId", commentId);
        params.put("text", text);
        callAction(Constants.Actions.UPDATE_COMMENT, params, callback);
    }

    public void deleteComment(String commentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("commentId", commentId);
        callAction(Constants.Actions.DELETE_COMMENT, params, callback);
    }

    public void getComments(String transactionId, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("transactionId", transactionId));
        queries.add(Query.Companion.orderAsc("$createdAt"));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_COMMENTS, queries, callback);
    }

    public void getLikes(String transactionId, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("transactionId", transactionId));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_LIKES, queries, callback);
    }

    public void listAllLikes(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.limit(100)); // Adjust limit as needed
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_LIKES, queries, callback);
    }

    // ─── MEMBERS ─────────────────────────────────────────────────────────────

    public void getMembers(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_USERS, queries, callback);
    }

    public void getPendingMembers(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("pendingFamilyId", familyId));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_USERS, queries, callback);
    }

    // ─── GOALS ───────────────────────────────────────────────────────────────

    public void addGoal(String name, double targetAmount, String targetDate, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", pref.getUsername());
        params.put("title", name);
        params.put("type", "GOAL");
        params.put("targetAmount", targetAmount);
        params.put("targetDate", targetDate);
        params.put("startDate", Utils.getCurrentTimestamp());
        callAction(Constants.Actions.ADD_GOAL, params, callback);
    }

    public void updateGoalAmount(String goalId, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("txnId", goalId);
        params.put("amount", amount);
        params.put("type", "GOAL");
        callAction(Constants.Actions.UPDATE_GOAL, params, callback);
    }

    public void deleteGoal(String goalId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("txnId", goalId);
        callAction(Constants.Actions.DELETE_GOAL, params, callback);
    }

    public void getGoalsSince(String sinceTimestamp, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }
        
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        queries.add(Query.Companion.equal("type", "GOAL"));
        queries.add(Query.Companion.greaterThan("$createdAt", sinceTimestamp));
        queries.add(Query.Companion.orderDesc("$createdAt"));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    public void getGoals(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        getGoalsSince(pref.getLastGoalSyncTime(), callback);
    }

    // ─── DEBTS ───────────────────────────────────────────────────────────────
    public void addDebt(String personName, double amount, String type, String category, String targetDate, String reminder, String notes, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("personName", personName);
        params.put("title", personName);
        params.put("amount", amount);
        params.put("type", "DEBT");
        params.put("debtType", type); 
        params.put("category", category);
        params.put("targetDate", targetDate);
        params.put("startDate", Utils.getCurrentTimestamp());
        params.put("reminder", reminder);
        params.put("notes", notes);
        callAction(Constants.Actions.ADD_DEBT, params, callback);
    }

    public void updateDebtAmount(String debtId, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("txnId", debtId);
        params.put("amount", amount);
        params.put("type", "DEBT");
        callAction(Constants.Actions.UPDATE_DEBT, params, callback);
    }

    public void markDebtPaid(String debtId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("txnId", debtId);
        params.put("isPaid", true);
        params.put("type", "DEBT");
        callAction(Constants.Actions.MARK_DEBT_PAID, params, callback);
    }

    public void deleteDebt(String debtId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("txnId", debtId);
        callAction(Constants.Actions.DELETE_DEBT, params, callback);
    }

    public void getDebtsSince(String sinceTimestamp, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        queries.add(Query.Companion.equal("type", "DEBT"));
        queries.add(Query.Companion.greaterThan("$createdAt", sinceTimestamp));
        queries.add(Query.Companion.orderDesc("$createdAt"));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    public void getDebts(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        getDebtsSince(pref.getLastDebtSyncTime(), callback);
    }

    // ─── INCOMES ─────────────────────────────────────────────────────────────

    public void addIncome(String source, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", pref.getUsername());
        params.put("title", source);
        params.put("source", source);
        params.put("amount", amount);
        params.put("type", "INCOME");
        params.put("category", "Income");
        params.put("startDate", Utils.getCurrentTimestamp());
        callAction(Constants.Actions.ADD_INCOME, params, callback);
    }

    public void updateIncome(String incomeId, String source, double amount, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("txnId", incomeId);
        params.put("title", source);
        params.put("source", source);
        params.put("amount", amount);
        params.put("type", "INCOME");
        callAction(Constants.Actions.UPDATE_INCOME, params, callback);
    }

    public void deleteIncome(String incomeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("txnId", incomeId);
        callAction(Constants.Actions.DELETE_INCOME, params, callback);
    }

    public void getIncomes(AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        queries.add(Query.Companion.equal("type", "INCOME"));
        queries.add(Query.Companion.orderDesc("$createdAt"));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    // ─── FAMILY SUMMARY ──────────────────────────────────────────────────────

    public void getFamilySummary(AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId == null || familyId.isEmpty() || "null".equals(familyId)) {
            callback.onError(new Exception("No family ID found"));
            return;
        }
        appwriteManager.getDocument(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_FAMILIES, familyId, callback);
    }

    public void updateFamilyTotals(double balance, double income, double expense, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("balance", balance);
        params.put("totalIncome", income);
        params.put("totalExpense", expense);
        callAction(Constants.Actions.UPDATE_FAMILY_TOTALS, params, callback);
    }

    public void sendMessage(String text, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        sendMessage(io.appwrite.ID.Companion.unique(0), text, pref.getUserId(), pref.getUsername(), callback);
    }

    public void sendMessageWithId(String id, String text, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        sendMessage(id, text, pref.getUserId(), pref.getUsername(), callback);
    }

    public void sendAiMessage(String text, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        sendMessage(io.appwrite.ID.Companion.unique(0), text, Constants.Roles.AI_ID, Constants.Roles.AI_NAME, callback);
    }

    private void sendMessage(String id, String text, String senderId, String senderName, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();

        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("senderId", senderId);
        data.put("senderName", senderName);
        data.put("familyId", familyId);
        data.put("timestamp", System.currentTimeMillis());

        List<String> permissions = new ArrayList<>();
        permissions.add(io.appwrite.Permission.Companion.read(io.appwrite.Role.Companion.users("")));

        appwriteManager.createDocument(
                Constants.Appwrite.DATABASE_ID,
                Constants.Appwrite.COL_MESSAGES,
                id,
                data,
                permissions,
                callback
        );
    }

    public void sendAnnouncement(String title, String content, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("content", content);
        callAction(Constants.Actions.SEND_ANNOUNCEMENT, params, callback);
    }

    public void getNotifications(String familyId, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        appwriteManager.listDocuments(
                Constants.Appwrite.DATABASE_ID,
                Constants.Appwrite.COL_NOTIFICATIONS,
                Arrays.asList(
                        Query.Companion.equal("familyId", familyId),
                        Query.Companion.orderDesc("$createdAt")
                ),
                callback
        );
    }
}
