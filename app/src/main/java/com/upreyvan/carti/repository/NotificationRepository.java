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
import com.upreyvan.carti.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Document;

public class NotificationRepository {

    private static NotificationRepository instance;
    private final RealtimeRepository realtimeRepo;
    private MediatorLiveData<List<Bill>> billsLiveData = new MediatorLiveData<>();
    private final List<Bill> currentBills = new ArrayList<>();
    private final MediatorLiveData<List<com.upreyvan.carti.models.Notification>> notificationsLiveData = new MediatorLiveData<>();
    private final List<com.upreyvan.carti.models.Notification> currentNotifications = new ArrayList<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final PreferenceManager pref;
    private String lastFamilyId = "";

    private NotificationRepository(Context context) {
        this.realtimeRepo = RealtimeRepository.getInstance(context);
        this.pref = PreferenceManager.getInstance(context);
    }

    public static synchronized NotificationRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<Map<String, Object>> getNotificationStream() {
        return realtimeRepo.getNotificationStream();
    }

    public LiveData<Map<String, Object>> getUserUpdateStream() {
        return realtimeRepo.getUserUpdateStream();
    }

    public void handleRealtimeEvent(Map<String, Object> data, boolean isDelete) {
        String id = (String) data.get("$id");
        if (id == null) return;

        synchronized (currentBills) {
            currentBills.removeIf(b -> id.equals(b.getId()));
            if (!isDelete) {
                Bill bill = mapToBill(id, data);
                if (bill != null) currentBills.add(0, bill);
            }
            billsLiveData.postValue(new ArrayList<>(currentBills));
        }

        synchronized (currentNotifications) {
            currentNotifications.removeIf(n -> id.equals(n.getId()));
            if (!isDelete) {
                com.upreyvan.carti.models.Notification n = mapToNotification(id, data);
                if (n != null) {
                    currentNotifications.add(0, n);
                    
                    long lastCheck = Utils.getMillisFromIso(pref.getLastNotifCheck());
                    if (n.getTimestamp() > lastCheck) {
                        Integer current = unreadCount.getValue();
                        unreadCount.postValue((current != null ? current : 0) + 1);
                    }
                }
            }
            notificationsLiveData.postValue(new ArrayList<>(currentNotifications));
        }
    }

    private com.upreyvan.carti.models.Notification mapToNotification(String id, Map<String, Object> data) {
        if (data == null) return null;
        String title = (String) data.get("title");
        String content = (String) data.get("content");
        String createdAt = (String) data.get("$createdAt");
        long ts = createdAt != null ? Utils.getMillisFromIso(createdAt) : System.currentTimeMillis();
        return new com.upreyvan.carti.models.Notification(id, title, content, ts);
    }

    public LiveData<Integer> getUnreadCount() { return unreadCount; }

    public void markAllAsRead() {
        pref.setLastNotifCheck(Utils.getCurrentTimestamp());
        unreadCount.postValue(0);
    }

    public LiveData<List<Bill>> getBills(String familyId) {
        if (billsLiveData == null || !familyId.equals(lastFamilyId)) {
            lastFamilyId = familyId;
            billsLiveData = new MediatorLiveData<>();
            currentBills.clear();

            new ApiHelper(realtimeRepo.getContext()).getNotifications(familyId, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
                @Override
                public void onSuccess(DocumentList<Map<String, Object>> result) {
                    currentBills.clear();
                    if (result != null && result.getDocuments() != null) {
                        for (Document<Map<String, Object>> doc : result.getDocuments()) {
                            Bill bill = mapToBill(doc.getId(), doc.getData());
                            if (bill != null) currentBills.add(bill);
                        }
                    }
                    billsLiveData.postValue(new ArrayList<>(currentBills));
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("NotificationRepository", "Fetch error: " + error.getMessage(), error);
                    billsLiveData.postValue(new ArrayList<>(currentBills));
                }
            });
            billsLiveData.addSource(realtimeRepo.getNotificationStream(), payload -> {
                if (payload != null && familyId.equals(payload.get("familyId"))) {
                    String id = (String) payload.get("$id");
                    Bill bill = mapToBill(id, payload);
                    if (bill != null) {
                        int index = -1;
                        for (int i = 0; i < currentBills.size(); i++) {
                            if (currentBills.get(i).getId().equals(id)) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            currentBills.set(index, bill);
                        } else {
                            currentBills.add(0, bill);
                        }
                        billsLiveData.setValue(new ArrayList<>(currentBills));
                    }
                }
            });
        }
        return billsLiveData;
    }

    public void listNotifications(String familyId, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) {
        AppwriteManager.getInstance(realtimeRepo.getContext()).listDocuments(
            com.upreyvan.carti.utils.Constants.Appwrite.DATABASE_ID,
            com.upreyvan.carti.utils.Constants.Appwrite.COL_NOTIFICATIONS,
            java.util.Arrays.asList(io.appwrite.Query.Companion.equal("familyId", familyId), io.appwrite.Query.Companion.orderDesc("$createdAt")),
            callback
        );
    }

    public void sendAnnouncement(String title, String content, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        new ApiHelper(realtimeRepo.getContext()).sendAnnouncement(title, content, callback);
    }

    public void deleteNotification(String id, AppwriteManager.AppwriteCallback<Object> callback) {
        AppwriteManager.getInstance(realtimeRepo.getContext()).deleteDocument(com.upreyvan.carti.utils.Constants.Appwrite.DATABASE_ID, com.upreyvan.carti.utils.Constants.Appwrite.COL_NOTIFICATIONS, id, callback);
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
                date = content.substring(index + 7).trim();
            } else {
                Object createdAt = data.get("$createdAt");
                if (createdAt != null) date = Utils.formatTimestamp(createdAt.toString());
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
