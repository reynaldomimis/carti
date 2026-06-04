package com.upreyvan.carti.util;

import android.content.Context;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import java.util.Map;

public class TransactionHandler {

    public interface TransactionCallback {
        void onLoading(boolean isLoading);
        void onSuccess(Transaction transaction);
        void onError(String message);
    }

    public static void saveTrack(Context context, double amount, String categoryName, String description, String source, TransactionCallback callback) {
        Context appContext = context.getApplicationContext();
        callback.onLoading(true);

        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setType("EXPENSE");
        t.setCategory(categoryName);
        t.setTitle(description + " (via " + source + ")");

        TransactionRepository.getInstance(appContext).addTransaction(t, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                callback.onLoading(false);
                TransactionRepository.getInstance(appContext).refreshTransactions();
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

        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setType("DEBT");
        t.setTitle(personName);
        t.setNote(note);
        t.setCategory("Personal");

        TransactionRepository.getInstance(appContext).addTransaction(t, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                callback.onLoading(false);
                TransactionRepository.getInstance(appContext).refreshTransactions();
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

        Transaction t = new Transaction();
        t.setAmount(0);
        t.setTargetAmount(amount);
        t.setType("GOAL");
        t.setTitle(goalName);

        TransactionRepository.getInstance(appContext).addTransaction(t, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                callback.onLoading(false);
                TransactionRepository.getInstance(appContext).refreshTransactions();
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
