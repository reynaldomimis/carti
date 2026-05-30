package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.LikeDao;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.model.Like;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Constants;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.appwrite.models.RealtimeSubscription;

public class RealtimeRepository {

    private static RealtimeRepository instance;
    private final RealtimeHelper realtimeHelper;
    private final PreferenceManager pref;
    private final LikeDao likeDao;
    private final TransactionDao transactionDao;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private RealtimeSubscription subscription;

    private final MutableLiveData<Map<String, Object>> transactionStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> goalStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> debtStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> notificationStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> userUpdateStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> familyStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> chatStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> incomeStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> commentStream = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> likeStream = new MutableLiveData<>();

    private RealtimeRepository(Context context) {
        this.realtimeHelper = new RealtimeHelper(context);
        this.pref = new PreferenceManager(context);
        AppDatabase db = AppDatabase.getInstance(context);
        this.likeDao = db.likeDao();
        this.transactionDao = db.transactionDao();
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

        String[] channels = {
            getCollectionChannel(Constants.Appwrite.COL_TRANSACTIONS),
            getCollectionChannel(Constants.Appwrite.COL_GOALS),
            getCollectionChannel(Constants.Appwrite.COL_DEBTS),
            getCollectionChannel(Constants.Appwrite.COL_NOTIFICATIONS),
            getCollectionChannel(Constants.Appwrite.COL_USERS),
            getCollectionChannel(Constants.Appwrite.COL_MESSAGES),
            getCollectionChannel(Constants.Appwrite.COL_INCOMES),
            getCollectionChannel(Constants.Appwrite.COL_COMMENTS),
            getCollectionChannel(Constants.Appwrite.COL_LIKES),
            getDocumentChannel(Constants.Appwrite.COL_FAMILIES, familyId)
        };

        subscription = realtimeHelper.subscribe(channels, event -> {
            Map<String, Object> payload = RealtimeHelper.getPayload(event);
            if (payload == null || event.getEvents().isEmpty()) return;

            String eventPath = event.getEvents().iterator().next();

            if (eventPath.contains(Constants.Appwrite.COL_LIKES)) {
                handleLikeEvent(eventPath, payload);
            } else if (isTransactionCollection(eventPath)) {
                if (familyId.equals(payload.get("familyId"))) {
                    handleTransactionEvent(eventPath, payload);
                    transactionStream.postValue(payload);
                    dispatchTypedStream(payload);
                }
            } else if (eventPath.contains(Constants.Appwrite.COL_NOTIFICATIONS)) {
                if (familyId.equals(payload.get("familyId"))) {
                    android.util.Log.d("RealtimeRepository", "Notification: " + payload.get("title"));
                    notificationStream.postValue(payload);
                }
            } else if (eventPath.contains(Constants.Appwrite.COL_MESSAGES)) {
                if (familyId.equals(payload.get("familyId"))) chatStream.postValue(payload);
            } else if (eventPath.contains(Constants.Appwrite.COL_USERS)) {
                userUpdateStream.postValue(payload);
            } else if (eventPath.contains(Constants.Appwrite.COL_FAMILIES)) {
                familyStream.postValue(payload);
            } else if (eventPath.contains(Constants.Appwrite.COL_COMMENTS)) {
                commentStream.postValue(payload);
            }
        });
    }

    private boolean isTransactionCollection(String path) {
        return path.contains(Constants.Appwrite.COL_TRANSACTIONS) ||
               path.contains(Constants.Appwrite.COL_GOALS) ||
               path.contains(Constants.Appwrite.COL_DEBTS) ||
               path.contains(Constants.Appwrite.COL_INCOMES);
    }

    private void dispatchTypedStream(Map<String, Object> payload) {
        String type = (String) payload.get("type");
        if ("INCOME".equals(type)) incomeStream.postValue(payload);
        else if ("GOAL".equals(type)) goalStream.postValue(payload);
        else if ("DEBT".equals(type)) debtStream.postValue(payload);
    }

    private void handleTransactionEvent(String eventPath, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                if (eventPath.contains(".delete")) {
                    String id = String.valueOf(payload.get("$id"));
                    transactionDao.deleteById(id);
                } else {
                    Transaction transaction = Transaction.fromPayload(payload, pref.getFamilyId(), getContext(), pref.getUserId());
                    if (transaction != null) {
                        transactionDao.insert(transaction);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("RealtimeRepository", "Error handling transaction event", e);
            }
        });
    }

    private void handleLikeEvent(String eventPath, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                String id = (String) payload.get("$id");
                String txnId = (String) payload.get("transactionId");
                String userId = (String) payload.get("userId");

                String username = (String) payload.get("username");
                if (username == null) username = (String) payload.get("userName");
                if (username != null) username = username.toLowerCase();
                
                String emoji = (String) payload.get("emojiType");

                if (id == null || txnId == null) return;

                if (eventPath.contains(".delete")) {
                    likeDao.deleteById(id);
                } else {
                    likeDao.insert(new Like(id, txnId, userId, username, emoji));
                }

                likeStream.postValue(payload);
            } catch (Exception e) {
                android.util.Log.e("RealtimeRepository", "Error handling like event", e);
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
    public LiveData<Map<String, Object>> getCommentStream() { return commentStream; }
    public LiveData<Map<String, Object>> getLikeStream() { return likeStream; }

    public void stopListening() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
