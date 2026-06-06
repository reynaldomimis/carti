package com.upreyvan.carti.data.repository;

import android.content.Context;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import io.appwrite.models.Session;
import java.util.Map;

public class AuthRepository {
    private static AuthRepository instance;
    private final ApiHelper apiHelper;
    private final AppwriteManager appwriteManager;

    private AuthRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
        this.appwriteManager = AppwriteManager.getInstance(context);
    }

    public static synchronized AuthRepository getInstance(Context context) {
        if (instance == null) instance = new AuthRepository(context.getApplicationContext());
        return instance;
    }

    public void login(String email, String password, AppwriteCallback<Session> callback) {
        appwriteManager.login(email, password, callback);
    }

    public void register(String email, String password, String username, boolean isEmployed, String role, AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.register(email, password, username, isEmployed, role, callback);
    }

    public void getUser(AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.getUser(callback);
    }

    public void getCurrentUser(AppwriteCallback<io.appwrite.models.User<Map<String, Object>>> callback) {
        appwriteManager.getCurrentUser(callback);
    }

    public void updatePasswordRecovery(String userId, String secret, String password, AppwriteCallback<Object> callback) {
        appwriteManager.updatePasswordRecovery(userId, secret, password, callback);
    }
}
