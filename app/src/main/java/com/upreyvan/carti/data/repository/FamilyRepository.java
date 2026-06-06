package com.upreyvan.carti.data.repository;

import android.content.Context;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.Map;

public class FamilyRepository {
    private static FamilyRepository instance;
    private final ApiHelper apiHelper;

    private FamilyRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
    }

    public static synchronized FamilyRepository getInstance(Context context) {
        if (instance == null) instance = new FamilyRepository(context.getApplicationContext());
        return instance;
    }

    public void getPendingMembers(AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        apiHelper.getPendingMembers(callback);
    }

    public void getFamilySummary(AppwriteCallback<Document<Map<String, Object>>> callback) {
        apiHelper.getFamilySummary(callback);
    }

    public void approveJoinRequest(String applicantId, AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.approveJoinRequest(applicantId, callback);
    }

    public void rejectJoinRequest(String applicantId, AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.rejectJoinRequest(applicantId, callback);
    }
}
