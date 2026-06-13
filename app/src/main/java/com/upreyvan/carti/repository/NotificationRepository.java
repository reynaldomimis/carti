package com.upreyvan.carti.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.upreyvan.carti.R;
import com.upreyvan.carti.datasource.ApiHelper;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.Notification;
import com.upreyvan.carti.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Document;

public class NotificationRepository {

    private static NotificationRepository instance;
    private final RealtimeRepository realtimeRepo;
    private final ApiHelper apiHelper;
    private MediatorLiveData<List<Bill>> billsLiveData = new MediatorLiveData<>();
    private final List<Bill> currentBills = new ArrayList<>();
    private final MediatorLiveData<List<Notification>> notificationsLiveData = new MediatorLiveData<>();
    private final List<Notification> currentNotifications = new ArrayList<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final PreferenceManager pref;
    private String lastFamilyId = "";

    private NotificationRepository(Context context) {
        this.realtimeRepo = RealtimeRepository.getInstance(context);
        this.apiHelper = new ApiHelper(context);
        this.pref = PreferenceManager.getInstance(context);
        
        refreshNotifications();
    }

    public static synchronized NotificationRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void handleRealtimeEvent(Map<String, Object> data, boolean isDelete) {
        String id = (String) data.get("$id");
        if (id == null) return;

        // Handle Bills
        synchronized (currentBills) {
            currentBills.removeIf(b -> id.equals(b.getId()));
            if (!isDelete) {
                Bill bill = mapToBill(id, data);
                if (bill != null) currentBills.add(0, bill);
            }
            billsLiveData.postValue(new ArrayList<>(currentBills));
        }

        // Handle Notifications
        synchronized (currentNotifications) {
            currentNotifications.removeIf(n -> id.equals(n.getId()));
            if (!isDelete) {
                Notification n = mapToNotification(id, data);
                if (n != null) {
                    currentNotifications.add(0, n);
                    
                    // Standard Software Engineering logic: 
                    // Skip system notification if user is already viewing the notifications list
                    if (com.upreyvan.carti.utils.AppLifecycleTracker.isNotificationsActive()) return;

                    // Show system notification for new items
                    com.upreyvan.carti.utils.NotificationHelper.showNotification(
                        realtimeRepo.getContext(), 
                        n.getTitle(), 
                        n.getContent()
                    );
                }
            }
            notificationsLiveData.postValue(new ArrayList<>(currentNotifications));
            updateUnreadCount();
        }
    }

    private void updateUnreadCount() {
        int count = 0;
        synchronized (currentNotifications) {
            for (Notification n : currentNotifications) {
                if (n.isUnread()) count++;
            }
        }
        unreadCount.postValue(count);
    }

    public LiveData<List<Notification>> getNotifications() {
        refreshNotifications();
        return notificationsLiveData;
    }

    public void refreshNotifications() {
        String userId = pref.getUserId();
        if (userId.isEmpty()) return;

        AppwriteManager.getInstance(realtimeRepo.getContext()).listDocuments(
                com.upreyvan.carti.utils.Constants.Appwrite.DATABASE_ID,
                com.upreyvan.carti.utils.Constants.Appwrite.COL_NOTIFICATIONS,
                java.util.Arrays.asList(
                        io.appwrite.Query.Companion.equal("targetUserId", userId),
                        io.appwrite.Query.Companion.orderDesc("timestamp")
                ),
                new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(DocumentList<Map<String, Object>> result) {
                        if (result != null && result.getDocuments() != null) {
                            synchronized (currentNotifications) {
                                currentNotifications.clear();
                                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                                    Notification n = mapToNotification(doc.getId(), doc.getData());
                                    if (n != null) currentNotifications.add(n);
                                }
                                notificationsLiveData.postValue(new ArrayList<>(currentNotifications));
                                updateUnreadCount();
                            }
                        }
                    }

                    @Override public void onError(Throwable error) {
                        Log.e("NotificationRepository", "Refresh error: " + error.getMessage());
                    }
                }
        );
    }

    public void markAsRead(String notificationId) {
        apiHelper.markNotificationRead(notificationId, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                synchronized (currentNotifications) {
                    for (Notification n : currentNotifications) {
                        if (n.getId().equals(notificationId)) {
                            n.setStatus("read");
                            break;
                        }
                    }
                    notificationsLiveData.postValue(new ArrayList<>(currentNotifications));
                    updateUnreadCount();
                }
            }
            @Override public void onError(Throwable error) {
                Log.e("NotificationRepository", "Mark as read error: " + error.getMessage());
            }
        });
    }

    public void markAllAsRead() {
        apiHelper.markAllNotificationsRead(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                synchronized (currentNotifications) {
                    for (Notification n : currentNotifications) {
                        n.setStatus("read");
                    }
                    notificationsLiveData.postValue(new ArrayList<>(currentNotifications));
                    updateUnreadCount();
                }
            }
            @Override public void onError(Throwable error) {}
        });
    }

    public LiveData<Integer> getUnreadCount() { return unreadCount; }

    public void sendAnnouncement(String title, String content, double amount, String category, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.sendAnnouncement(title, content, amount, category, callback);
    }

    public void deleteNotification(String id, AppwriteManager.AppwriteCallback<Object> callback) {
        apiHelper.deleteNotification(id, new AppwriteManager.AppwriteCallback<Object>() {
            @Override public void onSuccess(Object result) {
                refreshNotifications();
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Throwable error) { if (callback != null) callback.onError(error); }
        });
    }

    public void updateNotification(String id, Map<String, Object> data, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.updateNotification(id, data, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) {
                refreshNotifications();
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Throwable error) { if (callback != null) callback.onError(error); }
        });
    }

    public LiveData<List<Bill>> getBills(String familyId) {
        if (billsLiveData.getValue() == null || !familyId.equals(lastFamilyId)) {
            lastFamilyId = familyId;
            currentBills.clear();
            
            apiHelper.getNotifications(familyId, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
                @Override
                public void onSuccess(DocumentList<Map<String, Object>> result) {
                    synchronized (currentBills) {
                        currentBills.clear();
                        if (result != null && result.getDocuments() != null) {
                            for (Document<Map<String, Object>> doc : result.getDocuments()) {
                                Bill bill = mapToBill(doc.getId(), doc.getData());
                                if (bill != null) currentBills.add(bill);
                            }
                        }
                        billsLiveData.postValue(new ArrayList<>(currentBills));
                    }
                }

                @Override
                public void onError(Throwable error) {
                    billsLiveData.postValue(new ArrayList<>(currentBills));
                }
            });
        }
        return billsLiveData;
    }

    private Notification mapToNotification(String id, Map<String, Object> data) {
        if (data == null) return null;
        Notification n = new Notification();
        n.setId(id);
        n.setTitle((String) data.get("title"));
        n.setContent((String) data.get("content"));
        n.setType((String) data.get("type"));
        n.setFamilyId((String) data.get("familyId"));
        n.setTargetUserId((String) data.get("targetUserId"));
        n.setStatus((String) data.get("status"));
        n.setCategory((String) data.get("category"));
        n.setNotes((String) data.get("notes"));
        
        String tsStr = (String) data.get("timestamp");
        n.setTimestamp(tsStr);
        if (tsStr != null) n.setTimestampMillis(Utils.getMillisFromIso(tsStr));
        else {
            String createdAt = (String) data.get("$createdAt");
            if (createdAt != null) n.setTimestampMillis(Utils.getMillisFromIso(createdAt));
        }
        
        return n;
    }

    private Bill mapToBill(String id, Map<String, Object> data) {
        if (data == null) return null;
        
        String title = (String) data.get("title");
        if (title != null && title.toUpperCase().startsWith("BILL: ")) {
            String name = title.substring(6).trim();
            String content = (String) data.get("content");
            String date = "";
            
            if (content != null && content.toLowerCase().contains("due on ")) {
                int index = content.toLowerCase().lastIndexOf("due on ");
                String rawDatePart = content.substring(index + 7).trim();
                // Extract only the date part before the period or newline to avoid including notes
                if (rawDatePart.contains(".")) {
                    date = rawDatePart.substring(0, rawDatePart.indexOf(".")).trim();
                } else if (rawDatePart.contains("\n")) {
                    date = rawDatePart.substring(0, rawDatePart.indexOf("\n")).trim();
                } else {
                    date = rawDatePart;
                }
            } else {
                Object timestamp = data.get("timestamp");
                if (timestamp != null) date = Utils.formatTimestamp(timestamp.toString());
                else {
                    Object createdAt = data.get("$createdAt");
                    if (createdAt != null) date = Utils.formatTimestamp(createdAt.toString());
                }
            }

            double amount = 0;
            Object amtObj = data.get("amount");
            if (amtObj instanceof Number) amount = ((Number) amtObj).doubleValue();
            else if (amtObj instanceof String) {
                try { amount = Double.parseDouble((String) amtObj); } catch (Exception ignored) {}
            }

            String category = (String) data.get("category");
            if (category == null) category = "Bill";

            return new Bill(
                    id,
                    (String) data.get("familyId"),
                    name,
                    date,
                    "Upcoming",
                    R.drawable.ic_calendar,
                    amount,
                    category
            );
        }
        return null;
    }
}
