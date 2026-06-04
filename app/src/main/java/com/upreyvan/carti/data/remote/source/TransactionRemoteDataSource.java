package com.upreyvan.carti.data.remote.source;

import android.content.Context;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.appwrite.Query;
import io.appwrite.models.DocumentList;

public class TransactionRemoteDataSource {
    private final ApiHelper apiHelper;
    private final AppwriteManager appwriteManager;
    private final PreferenceManager pref;

    public TransactionRemoteDataSource(Context context) {
        this.apiHelper = new ApiHelper(context);
        this.appwriteManager = AppwriteManager.getInstance(context);
        this.pref = PreferenceManager.getInstance(context);
    }

    public void addTransaction(Map<String, Object> data, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.callAction(Constants.Actions.CREATE_TRANSACTION, data, callback);
    }

    public void updateTransaction(String id, Map<String, Object> data, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        data.put("txnId", id);
        apiHelper.callAction(Constants.Actions.UPDATE_TRANSACTION, data, callback);
    }

    public void deleteTransaction(String id, AppwriteManager.AppwriteCallback<Object> callback) {
        apiHelper.deleteTransaction(id, callback);
    }

    public void getTransactionsSince(String lastSync, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) { callback.onError(new Exception(Constants.ErrorCodes.NO_FAMILY)); return; }

        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("familyId", familyId));
        if (lastSync != null && !lastSync.isEmpty()) queries.add(Query.Companion.greaterThan("$updatedAt", lastSync));
        queries.add(Query.Companion.orderDesc("$updatedAt"));
        queries.add(Query.Companion.limit(1000));
        appwriteManager.listDocuments(Constants.Appwrite.DATABASE_ID, Constants.Appwrite.COL_TRANSACTIONS, queries, callback);
    }

    public void addLike(String transactionId, String emoji, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.addLike(transactionId, emoji, callback);
    }

    public void removeLike(String likeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.removeLike(likeId, callback);
    }

    public void addComment(String transactionId, String text, String parentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.addComment(transactionId, text, parentId, callback);
    }

    public void deleteComment(String commentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.deleteComment(commentId, callback);
    }

    public void getComments(String transactionId, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        apiHelper.getComments(transactionId, callback);
    }
}
