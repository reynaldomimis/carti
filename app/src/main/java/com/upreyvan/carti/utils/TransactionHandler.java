package com.upreyvan.carti.utils;

import android.content.Context;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionType;
import java.util.Map;

public class TransactionHandler {

    public interface TransactionCallback {
        void onLoading(boolean isLoading);
        void onSuccess(Transaction transaction);
        void onError(String message);
    }

    public static void saveTrack(Context context, double amount, String categoryName, String description, String source, TransactionCallback callback) {
        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setCategory(categoryName);
        t.setTitle(description + " (via " + source + ")");
        saveItem(context, TransactionType.EXPENSE, t, callback);
    }

    public static void saveDebt(Context context, double amount, String personName, String note, TransactionCallback callback) {
        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setTitle(personName);
        t.setNote(note);
        t.setCategory("Personal");
        saveItem(context, TransactionType.DEBT, t, callback);
    }

    public static void saveGoal(Context context, double amount, String goalName, TransactionCallback callback) {
        Transaction t = new Transaction();
        t.setAmount(0);
        t.setTargetAmount(amount);
        t.setTitle(goalName);
        saveItem(context, TransactionType.GOAL, t, callback);
    }

    private static void saveItem(Context context, TransactionType type, Transaction transaction, TransactionCallback callback) {
        Context appContext = context.getApplicationContext();
        callback.onLoading(true);

        TransactionRepository.getInstance(appContext).createItem(type, transaction, new AppwriteManager.AppwriteCallback<>() {
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
