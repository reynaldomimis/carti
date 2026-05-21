package com.upreyvan.carti.util;

import android.content.Context;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Transaction;
import java.util.Map;

public class TransactionHandler {

    public interface TransactionCallback {
        void onLoading(boolean isLoading);
        void onSuccess(Transaction transaction);
        void onError(String message);
    }

    /**
     * SENDS TO SERVER ONLY.
     * Room is updated automatically via Realtime in TransactionRepository.
     */
    public static void saveTrack(Context context, double amount, String categoryName, String description, String source, TransactionCallback callback) {
        Context appContext = context.getApplicationContext();
        callback.onLoading(true);

        new ApiHelper(appContext).addTransaction(amount, "EXPENSE", categoryName, description + " (via " + source + ")", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                callback.onLoading(false);
                callback.onSuccess(null); 
            }

            @Override
            public void onError(Throwable error) {
                callback.onLoading(false);
                callback.onError(error.getMessage());
            }
        });
    }

    public static void saveDebt(Context context, double amount, String personName, String note, TransactionCallback callback) {
        Context appContext = context.getApplicationContext();
        callback.onLoading(true);

        new ApiHelper(appContext).addDebt(personName, amount, "I Owe", "Personal", "", "", note, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                callback.onLoading(false);
                callback.onSuccess(null);
            }

            @Override
            public void onError(Throwable error) {
                callback.onLoading(false);
                callback.onError(error.getMessage());
            }
        });
    }

    public static void saveGoal(Context context, double amount, String goalName, TransactionCallback callback) {
        Context appContext = context.getApplicationContext();
        callback.onLoading(true);

        new ApiHelper(appContext).addGoal(goalName, amount, "", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                callback.onLoading(false);
                callback.onSuccess(null);
            }

            @Override
            public void onError(Throwable error) {
                callback.onLoading(false);
                callback.onError(error.getMessage());
            }
        });
    }
}
