package com.upreyvan.carti.repository;

import android.content.Context;
import com.upreyvan.carti.datasource.ApiHelper;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import java.util.Map;

public class OnboardingRepository {
    private static OnboardingRepository instance;
    private final ApiHelper apiHelper;

    private OnboardingRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
    }

    public static synchronized OnboardingRepository getInstance(Context context) {
        if (instance == null) instance = new OnboardingRepository(context.getApplicationContext());
        return instance;
    }

    public void getUserStatus(AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.getUser(callback);
    }

    public void createFamily(String name, AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.createFamily(name, callback);
    }

    public void joinFamily(String inviteCode, AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.joinFamily(inviteCode, callback);
    }
}
