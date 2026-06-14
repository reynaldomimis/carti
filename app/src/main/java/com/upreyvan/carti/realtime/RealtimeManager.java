package com.upreyvan.carti.realtime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.MemberRepository;
import com.upreyvan.carti.repository.NotificationRepository;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.utils.Constants;

import java.util.Collection;
import java.util.Map;

import io.appwrite.models.RealtimeSubscription;

public class RealtimeManager {
    private static RealtimeManager instance;

    private final Context context;
    private final RealtimeHelper realtimeHelper;
    private final PreferenceManager pref;

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

    private RealtimeManager(Context context) {
        this.context = context.getApplicationContext();
        this.realtimeHelper = new RealtimeHelper(this.context);
        this.pref = PreferenceManager.getInstance(this.context);
    }

    public static synchronized RealtimeManager getInstance(Context context) {
        if (instance == null) instance = new RealtimeManager(context);
        return instance;
    }

    public synchronized void startListening() {
        if (subscription != null) return;
        
        String familyId = pref.getFamilyId();
        java.util.List<String> channelList = new java.util.ArrayList<>();
        
        // Essential channels for all users (including those onboarding)
        channelList.add(RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_USERS));
        
        // Channels that require a familyId
        if (familyId != null && !familyId.isEmpty()) {
            channelList.add(RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_TRANSACTIONS));
            channelList.add(RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_NOTIFICATIONS));
            channelList.add(RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_MESSAGES));
            channelList.add(RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_COMMENTS));
            channelList.add(RealtimeHelper.getCollectionChannel(Constants.Appwrite.COL_LIKES));
            channelList.add(RealtimeHelper.getDocumentChannel(Constants.Appwrite.COL_FAMILIES, familyId));
        }

        String[] channels = channelList.toArray(new String[0]);
        subscription = realtimeHelper.subscribe(channels, event -> {
            Map<String, Object> payload = RealtimeHelper.getPayload(event);
            Collection<String> events = event.getEvents();
            if (payload == null || events == null || events.isEmpty()) return;

            String path = events.iterator().next();
            if (!belongsToCurrentFamily(path, payload, familyId)) return;

            dispatch(path, payload);
        });
    }

    private boolean belongsToCurrentFamily(String path, Map<String, Object> payload, String familyId) {
        // User updates are personalized or global, ignore family check during onboarding
        if (path.contains(Constants.Appwrite.COL_USERS)) return true;
        
        boolean isGlobal = path.contains(Constants.Appwrite.COL_FAMILIES);
        if (isGlobal) return true;

        if (familyId == null || familyId.isEmpty()) return false;

        Object payloadFamilyId = payload.get("familyId");
        return familyId.equals(String.valueOf(payloadFamilyId));
    }

    private void dispatch(String path, Map<String, Object> payload) {
        boolean isDelete = path.endsWith(".delete");

        if (path.contains(Constants.Appwrite.COL_LIKES)) {
            likeStream.postValue(payload);
            TransactionRepository.getInstance(context).handleLikeEventLocally(payload, isDelete);
        } else if (path.contains(Constants.Appwrite.COL_TRANSACTIONS)) {
            transactionStream.postValue(payload);
            dispatchTypedTransaction(payload);
            TransactionRepository.getInstance(context).handleRealtimeEvent(payload, isDelete);
        } else if (path.contains(Constants.Appwrite.COL_NOTIFICATIONS)) {
            notificationStream.postValue(payload);
            NotificationRepository.getInstance(context).handleRealtimeEvent(payload, isDelete);
        } else if (path.contains(Constants.Appwrite.COL_MESSAGES)) {
            chatStream.postValue(payload);
            if (!isDelete) {
                String senderId = (String) payload.get("senderId");
                if (!pref.getUserId().equals(senderId)) {
                    // Suppression Rule: Skip tray notification ONLY if user is already in the chat room
                    if (com.upreyvan.carti.utils.AppLifecycleTracker.isChatActive()) return;

                    String senderName = (String) payload.get("senderName");
                    String text = (String) payload.get("text");
                    com.upreyvan.carti.utils.NotificationHelper.showNotification(
                        context, 
                        senderName != null ? senderName : "New Message", 
                        text,
                        true
                    );
                }
            }
        } else if (path.contains(Constants.Appwrite.COL_USERS)) {
            userUpdateStream.postValue(payload);
            MemberRepository.getInstance(context).refreshMembers();
        } else if (path.contains(Constants.Appwrite.COL_COMMENTS)) {
            commentStream.postValue(payload);
            TransactionRepository.getInstance(context).handleCommentEventLocally(payload, isDelete);
        }
    }

    private void dispatchTypedTransaction(Map<String, Object> payload) {
        String type = (String) payload.get("type");
        if ("INCOME".equals(type)) incomeStream.postValue(payload);
        else if ("GOAL".equals(type)) goalStream.postValue(payload);
        else if ("DEBT".equals(type)) debtStream.postValue(payload);
    }

    public synchronized void stopListening() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
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
}
