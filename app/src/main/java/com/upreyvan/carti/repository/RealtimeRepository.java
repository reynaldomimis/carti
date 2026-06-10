package com.upreyvan.carti.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.upreyvan.carti.realtime.RealtimeManager;

import java.util.Map;

public class RealtimeRepository {
    private static RealtimeRepository instance;

    private final Context context;
    private final RealtimeManager realtimeManager;

    private RealtimeRepository(Context context) {
        this.context = context.getApplicationContext();
        this.realtimeManager = RealtimeManager.getInstance(this.context);
    }

    public static synchronized RealtimeRepository getInstance(Context context) {
        if (instance == null) instance = new RealtimeRepository(context);
        return instance;
    }

    public void startListening() {
        realtimeManager.startListening();
    }

    public void stopListening() {
        realtimeManager.stopListening();
    }

    public LiveData<Map<String, Object>> getTransactionStream() { return realtimeManager.getTransactionStream(); }
    public LiveData<Map<String, Object>> getGoalStream() { return realtimeManager.getGoalStream(); }
    public LiveData<Map<String, Object>> getDebtStream() { return realtimeManager.getDebtStream(); }
    public LiveData<Map<String, Object>> getNotificationStream() { return realtimeManager.getNotificationStream(); }
    public LiveData<Map<String, Object>> getUserUpdateStream() { return realtimeManager.getUserUpdateStream(); }
    public LiveData<Map<String, Object>> getChatStream() { return realtimeManager.getChatStream(); }
    public LiveData<Map<String, Object>> getIncomeStream() { return realtimeManager.getIncomeStream(); }
    public LiveData<Map<String, Object>> getCommentStream() { return realtimeManager.getCommentStream(); }
    public LiveData<Map<String, Object>> getLikeStream() { return realtimeManager.getLikeStream(); }

    public Context getContext() { return context; }
}
