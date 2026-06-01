package com.upreyvan.carti.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.LikeDao;
import com.upreyvan.carti.data.local.source.TransactionLocalDataSource;
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.model.Like;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Constants;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import io.appwrite.models.RealtimeSubscription;

public class RealtimeRepository {
    private static final String TAG = "RealtimeRepository";
    private static RealtimeRepository instance;
    
    private final RealtimeHelper realtimeHelper;
    private final PreferenceManager pref;
    private final LikeDao likeDao;
    private final TransactionLocalDataSource localDataSource;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Context context;
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
        this.context = context.getApplicationContext();
        this.realtimeHelper = new RealtimeHelper(context);
        this.pref = new PreferenceManager(context);
        AppDatabase db = AppDatabase.getInstance(context);
        this.likeDao = db.likeDao();
        this.localDataSource = new TransactionLocalDataSource(context);
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
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_GOALS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_DEBTS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_NOTIFICATIONS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_USERS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_MESSAGES),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_INCOMES),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_COMMENTS),
            RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_LIKES),
            RealtimeHelper.getDocumentChannel(Constants.Appwrite.COL_FAMILIES, familyId)
        };

        subscription = realtimeHelper.subscribe(channels, event -> {
            Map<String, Object> payload = RealtimeHelper.getPayload(event);
            Collection<String> events = event.getEvents();
            if (payload == null || events.isEmpty()) return;

            String path = events.iterator().next();
            String payloadFamilyId = (String) payload.get("familyId");
            boolean isGlobal = path.contains(Constants.Appwrite.COL_FAMILIES) || path.contains(Constants.Appwrite.COL_USERS);

            if (!isGlobal && !familyId.equals(payloadFamilyId)) return;

            if (path.contains(Constants.Appwrite.COL_LIKES)) handleLikeEvent(path, payload);
            else if (isTransactionCollection(path)) handleTransactionEvent(path, payload);
            else if (path.contains(Constants.Appwrite.COL_NOTIFICATIONS)) notificationStream.postValue(payload);
            else if (path.contains(Constants.Appwrite.COL_MESSAGES)) chatStream.postValue(payload);
            else if (path.contains(Constants.Appwrite.COL_USERS)) userUpdateStream.postValue(payload);
            else if (path.contains(Constants.Appwrite.COL_FAMILIES)) familyStream.postValue(payload);
            else if (path.contains(Constants.Appwrite.COL_COMMENTS)) commentStream.postValue(payload);
        });
    }

    private boolean isTransactionCollection(String path) {
        return path.contains(Constants.Appwrite.COL_TRANSACTIONS) ||
               path.contains(Constants.Appwrite.COL_GOALS) ||
               path.contains(Constants.Appwrite.COL_DEBTS) ||
               path.contains(Constants.Appwrite.COL_INCOMES);
    }

    private void handleTransactionEvent(String path, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                if (path.endsWith(".delete")) {
                    localDataSource.deleteTransactionById(String.valueOf(payload.get("$id")));
                } else {
                    Transaction t = Transaction.fromPayload(payload, pref.getFamilyId(), context, pref.getUserId());
                    if (t != null) localDataSource.saveTransactions(java.util.Collections.singletonList(t));
                }
                transactionStream.postValue(payload);
                dispatchTypedStream(payload);
            } catch (Exception e) {
                Log.e(TAG, "Transaction event error", e);
            }
        });
    }

    private void dispatchTypedStream(Map<String, Object> payload) {
        String type = (String) payload.get("type");
        if ("INCOME".equals(type)) incomeStream.postValue(payload);
        else if ("GOAL".equals(type)) goalStream.postValue(payload);
        else if ("DEBT".equals(type)) debtStream.postValue(payload);
    }

    private void handleLikeEvent(String path, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                String id = (String) payload.get("$id");
                if (path.endsWith(".delete")) {
                    likeDao.deleteById(id);
                } else {
                    String txnId = (String) payload.get("transactionId");
                    String userId = (String) payload.get("userId");
                    String username = (String) payload.get("username");
                    if (username == null) username = (String) payload.get("userName");
                    if (username != null) username = username.toLowerCase();
                    String emoji = (String) payload.get("emojiType");
                    if (id != null && txnId != null) likeDao.insert(new Like(id, txnId, userId, username, emoji));
                }
                likeStream.postValue(payload);
            } catch (Exception e) {
                Log.e(TAG, "Like event error", e);
            }
        });
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

    public Context getContext() { return context; }

    public synchronized void stopListening() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
