package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.util.Constants;
import java.util.Collection;
import java.util.Map;
import io.appwrite.models.RealtimeSubscription;

public class RealtimeRepository {
    private static final String TAG = "RealtimeRepository";
    private static RealtimeRepository instance;
    
    private final RealtimeHelper realtimeHelper;
    private final PreferenceManager pref;
    private final Context context;
    private RealtimeSubscription subscription;

    private final MutableLiveData<Map<String, Object>> transactionStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> goalStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> debtStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> notificationStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> userUpdateStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> chatStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> incomeStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> commentStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> likeStream = new MutableLiveData<>();

    private RealtimeRepository(Context context) {
        this.context = context.getApplicationContext();
        this.realtimeHelper = new RealtimeHelper(context);
        this.pref = PreferenceManager.getInstance(context);
    }

    public static synchronized RealtimeRepository getInstance(Context context) {
        if (instance == null) instance = new RealtimeRepository(context);
        return instance;
    }

    public synchronized void startListening() {
        String familyId = pref.getFamilyId();
        if (familyId == null || familyId.isEmpty() || subscription != null) return;

        String[] channels = {
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_TRANSACTIONS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_NOTIFICATIONS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_USERS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_MESSAGES),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_COMMENTS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_LIKES),
            RealtimeHelper.getDocumentChannel(Constants.Appwrite.COL_FAMILIES, familyId)
        };

        subscription = realtimeHelper.subscribe(channels, event -> {
            Map<String, Object> payload = RealtimeHelper.getPayload(event);
            Collection<String> events = event.getEvents();
            if (payload == null || events.isEmpty()) return;

            String path = events.iterator().next();
            String payloadFamilyId = String.valueOf(payload.get("familyId"));
            boolean isGlobal = path.contains(Constants.Appwrite.COL_FAMILIES) || path.contains(Constants.Appwrite.COL_USERS);

            if (!isGlobal && !familyId.equals(payloadFamilyId)) return;

            if (path.contains(Constants.Appwrite.COL_LIKES)) {
                likeStream.postValue(payload);
                TransactionRepository.getInstance(context).handleLikeEventLocally(payload, path.endsWith(".delete"));
            } else if (path.contains(Constants.Appwrite.COL_FAMILIES)) {
                // Family document updated
            } else if (path.contains(Constants.Appwrite.COL_TRANSACTIONS)) {
                transactionStream.postValue(payload);
                dispatchTypedStream(payload);
                TransactionRepository.getInstance(context).handleRealtimeEvent(payload, path.endsWith(".delete"));
            } else if (path.contains(Constants.Appwrite.COL_NOTIFICATIONS)) {
                notificationStream.postValue(payload);
            } else if (path.contains(Constants.Appwrite.COL_MESSAGES)) {
                chatStream.postValue(payload);
            } else if (path.contains(Constants.Appwrite.COL_USERS)) {
                userUpdateStream.postValue(payload);
                MemberRepository.getInstance(context).refreshMembers();
            } else if (path.contains(Constants.Appwrite.COL_COMMENTS)) {
                commentStream.postValue(payload);
                TransactionRepository.getInstance(context).handleCommentEventLocally(payload, path.endsWith(".delete"));
            }
        });
    }

    private void dispatchTypedStream(Map<String, Object> payload) {
        String type = (String) payload.get("type");
        if ("INCOME".equals(type)) incomeStream.postValue(payload);
        else if ("GOAL".equals(type)) goalStream.postValue(payload);
        else if ("DEBT".equals(type)) debtStream.postValue(payload);
    }

    public LiveData<Map<String, Object>> getTransactionStream() { return transactionStream; }
    public LiveData<Map<String, Object>> getGoalStream() { return goalStream; }
    public LiveData<Map<String, Object>> getDebtStream() { return debtStream; }
    public LiveData<Map<String, Object>> getNotificationStream() { return notificationStream; }
    public LiveData<Map<String, Object>> getUserUpdateStream() { return userUpdateStream; }
    public LiveData<Map<String, Object>> getChatStream() { return chatStream; }
    public LiveData<Map<String, Object>> getIncomeStream() { return incomeStream; }
    public LiveData<Map<String, Object>> getCommentStream() { return commentStream; }
    public LiveData<Map<String, Object>> getLikeStream() { return likeStream; }

    public Context getContext() { return context; }

    public synchronized void stopListening() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
