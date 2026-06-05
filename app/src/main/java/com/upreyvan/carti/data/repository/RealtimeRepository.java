package com.upreyvan.carti.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.CommentDao;
import com.upreyvan.carti.data.local.db.dao.LikeDao;
import com.upreyvan.carti.data.local.db.dao.MessageDao;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.util.CommentHelper;
import com.upreyvan.carti.util.MessageHelper;
import com.upreyvan.carti.data.local.source.TransactionLocalDataSource;
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.model.Like;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Constants;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import io.appwrite.models.RealtimeSubscription;

public class RealtimeRepository {
    private static final String TAG = "RealtimeRepository";
    private static RealtimeRepository instance;
    
    private final RealtimeHelper realtimeHelper;
    private final PreferenceManager pref;
    private final LikeDao likeDao;
    private final MessageDao messageDao;
    private final CommentDao commentDao;
    private final TransactionLocalDataSource localDataSource;
    private final Executor executor = Executors.newSingleThreadExecutor();
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
        AppDatabase db = AppDatabase.getInstance(context);
        this.likeDao = db.likeDao();
        this.messageDao = db.messageDao();
        this.commentDao = db.commentDao();
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
                handleLikeEvent(path, payload);
                likeStream.postValue(payload);
            } else if (path.contains(Constants.Appwrite.COL_FAMILIES)) {
                handleFamilyEvent(path, payload);
            } else if (isTransactionCollection(path)) {
                handleTransactionEvent(path, payload);
            } else if (path.contains(Constants.Appwrite.COL_NOTIFICATIONS)) {
                notificationStream.postValue(payload);
            } else if (path.contains(Constants.Appwrite.COL_MESSAGES)) {
                handleChatMessageEvent(path, payload);
                chatStream.postValue(payload);
            } else if (path.contains(Constants.Appwrite.COL_USERS)) {
                userUpdateStream.postValue(payload);
            } else if (path.contains(Constants.Appwrite.COL_COMMENTS)) {
                handleCommentEvent(path, payload);
                commentStream.postValue(payload);
            }
        });
    }

    private void handleCommentEvent(String path, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                String id = String.valueOf(payload.get("$id"));
                if (path.endsWith(".delete")) {
                    commentDao.deleteById(id);
                } else {
                    Comment comment = CommentHelper.parse(payload);
                    if (comment != null) commentDao.insert(comment);
                }
            } catch (Exception e) {
                Log.e(TAG, "Comment event error", e);
            }
        });
    }

    private boolean isTransactionCollection(String path) {
        return path.contains(Constants.Appwrite.COL_TRANSACTIONS);
    }

    private void handleChatMessageEvent(String path, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                String id = String.valueOf(payload.get("$id"));
                if (path.endsWith(".delete")) {
                    messageDao.deleteById(id);
                } else {
                    ChatMessage msg = MessageHelper.mapToChatMessage(payload, id, pref.getUserId(), pref.getUsername());
                    if (msg != null) messageDao.insert(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Chat event error", e);
            }
        });
    }

    private void handleFamilyEvent(String path, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                Object bp = payload.get("budgetPlan");
                if (bp != null) {
                    com.upreyvan.carti.data.local.BudgetManager.getInstance(context).saveBudgetPlanFromJson(String.valueOf(bp));
                }
            } catch (Exception e) {
                Log.e(TAG, "Family event error", e);
            }
        });
    }

    private void handleTransactionEvent(String path, Map<String, Object> payload) {
        executor.execute(() -> {
            try {
                String id = String.valueOf(payload.get("$id"));
                if (path.endsWith(".delete")) {
                    localDataSource.deleteTransactionById(id);
                } else {
                    String remoteUpdated = String.valueOf(payload.get("$updatedAt"));
                    Transaction existing = AppDatabase.getInstance(context).transactionDao().getTransactionByIdRawSync(id);
                    
                    if (existing != null && Objects.equals(existing.getUpdatedAt(), remoteUpdated)) {
                        return;
                    }

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
                String id = String.valueOf(payload.get("$id"));
                if (path.endsWith(".delete")) {
                    likeDao.deleteById(id);
                } else {
                    String txnId = String.valueOf(payload.get("transactionId"));
                    String userId = String.valueOf(payload.get("userId"));
                    String remoteUpdated = String.valueOf(payload.get("$updatedAt"));
                    
                    Like existing = likeDao.getLikeByUserAndTransaction(txnId, userId);
                    if (existing != null && existing.getUpdatedAt() != null && 
                        remoteUpdated != null && !remoteUpdated.equals("null") &&
                        remoteUpdated.compareTo(existing.getUpdatedAt()) < 0) {
                        return;
                    }

                    String username = String.valueOf(payload.get("username") != null ? payload.get("username") : payload.get("userName"));
                    if (username != null && !username.equals("null")) username = username.toLowerCase();
                    String emoji = String.valueOf(payload.get("emojiType"));
                    
                    if (id != null && !id.equals("null") && txnId != null && !txnId.equals("null")) {
                        Like like = new Like(id, txnId, userId, username, emoji);
                        like.setUpdatedAt(remoteUpdated);
                        likeDao.insert(like);
                    }
                }
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
