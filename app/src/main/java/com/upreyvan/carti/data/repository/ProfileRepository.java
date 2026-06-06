package com.upreyvan.carti.data.repository;

import android.content.Context;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import io.appwrite.models.DocumentList;
import java.util.Map;

public class ProfileRepository {
    private static ProfileRepository instance;
    private final ApiHelper apiHelper;
    private final AppwriteManager appwriteManager;

    private ProfileRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
        this.appwriteManager = AppwriteManager.getInstance(context);
    }

    public static synchronized ProfileRepository getInstance(Context context) {
        if (instance == null) instance = new ProfileRepository(context.getApplicationContext());
        return instance;
    }

    public void logout(AppwriteCallback<Object> callback) {
        appwriteManager.logout(callback);
    }

    public void logoutAll(AppwriteCallback<Object> callback) {
        appwriteManager.logoutAll(callback);
    }

    public void updatePassword(String newPassword, String oldPassword, AppwriteCallback<io.appwrite.models.User<java.util.Map<String, Object>>> callback) {
        appwriteManager.updatePassword(newPassword, oldPassword, callback);
    }

    public void deleteAccount(AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.deleteAccount(callback);
    }

    public void getMembers(AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        apiHelper.getMembers(callback);
    }
}
