package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.util.Constants;
import java.util.Map;
import io.appwrite.models.RealtimeSubscription;

/**
 * ELITE ARCHITECTURE: The Central Realtime Hub.
 * This repository manages a SINGLE websocket connection for the entire app,
 * routing data updates for Transactions, Goals, Debts, Notifications, and Family state.
 */
public class RealtimeRepository {

    private static RealtimeRepository instance;
    private final RealtimeHelper realtimeHelper;
    private final PreferenceManager pref;
    private RealtimeSubscription subscription;

    // Specialized streams for different modules
    private final MutableLiveData<Map<String, Object>> transactionStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> goalStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> debtStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> notificationStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> userUpdateStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> familyStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> chatStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> incomeStream = new MutableLiveData<>();

    private RealtimeRepository(Context context) {
        this.realtimeHelper = new RealtimeHelper(context);
        this.pref = new PreferenceManager(context);
    }

    public Context getContext() {
        return realtimeHelper.getContext();
    }

    public static synchronized RealtimeRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RealtimeRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void startListening() {
        String familyId = pref.getFamilyId();
        if (familyId == null || familyId.isEmpty() || subscription != null) return;

        // One connection to rule them all
        String[] channels = {
            getCollectionChannel(Constants.Appwrite.COL_TRANSACTIONS),
            getCollectionChannel(Constants.Appwrite.COL_GOALS),
            getCollectionChannel(Constants.Appwrite.COL_DEBTS),
            getCollectionChannel(Constants.Appwrite.COL_NOTIFICATIONS),
            getCollectionChannel(Constants.Appwrite.COL_USERS),
            getCollectionChannel(Constants.Appwrite.COL_MESSAGES),
            getCollectionChannel(Constants.Appwrite.COL_INCOMES),
            getDocumentChannel(Constants.Appwrite.COL_FAMILIES, familyId)
        };

        subscription = realtimeHelper.subscribe(channels, event -> {
            Map<String, Object> payload = RealtimeHelper.getPayload(event);
            if (payload == null) return;

            String eventPath = "";
            if (!event.getEvents().isEmpty()) {
                eventPath = event.getEvents().iterator().next();
            }

            if (eventPath.contains(Constants.Appwrite.COL_TRANSACTIONS)) {
                if (familyId.equals(payload.get("familyId"))) {
                    transactionStream.postValue(payload);
                    
                    // Unified collection mapping
                    String type = (String) payload.get("type");
                    if ("INCOME".equals(type)) incomeStream.postValue(payload);
                    else if ("GOAL".equals(type)) goalStream.postValue(payload);
                    else if ("DEBT".equals(type)) debtStream.postValue(payload);
                }
            } 
            else if (eventPath.contains(Constants.Appwrite.COL_GOALS)) {
                if (familyId.equals(payload.get("familyId"))) goalStream.postValue(payload);
            } 
            else if (eventPath.contains(Constants.Appwrite.COL_DEBTS)) {
                if (familyId.equals(payload.get("familyId"))) debtStream.postValue(payload);
            } 
            else if (eventPath.contains(Constants.Appwrite.COL_NOTIFICATIONS)) {
                if (familyId.equals(payload.get("familyId"))) {
                    android.util.Log.d("RealtimeRepository", "Notification received: " + payload.get("title"));
                    notificationStream.postValue(payload);
                }
            } 
            else if (eventPath.contains(Constants.Appwrite.COL_USERS)) {
                userUpdateStream.postValue(payload);
            }
            else if (eventPath.contains(Constants.Appwrite.COL_FAMILIES)) {
                familyStream.postValue(payload);
            }
            else if (eventPath.contains(Constants.Appwrite.COL_MESSAGES)) {
                if (familyId.equals(payload.get("familyId"))) chatStream.postValue(payload);
            }
            else if (eventPath.contains(Constants.Appwrite.COL_INCOMES)) {
                if (familyId.equals(payload.get("familyId"))) incomeStream.postValue(payload);
            }
        });
    }

    private String getCollectionChannel(String collectionId) {
        return "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents";
    }

    private String getDocumentChannel(String collectionId, String documentId) {
        return "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents." + documentId;
    }

    public LiveData<Map<String, Object>> getTransactionStream() { return transactionStream; }
    public LiveData<Map<String, Object>> getGoalStream() { return goalStream; }
    public LiveData<Map<String, Object>> getDebtStream() { return debtStream; }
    public LiveData<Map<String, Object>> getNotificationStream() { return notificationStream; }
    public LiveData<Map<String, Object>> getUserUpdateStream() { return userUpdateStream; }
    public LiveData<Map<String, Object>> getFamilyStream() { return familyStream; }
    public LiveData<Map<String, Object>> getChatStream() { return chatStream; }
    public LiveData<Map<String, Object>> getIncomeStream() { return incomeStream; }

    public void stopListening() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
