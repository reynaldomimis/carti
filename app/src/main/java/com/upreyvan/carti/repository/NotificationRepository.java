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
    private final MediatorLiveData<List<Bill>> billsLiveData = new MediatorLiveData<>();
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
                    
                    String type = n.getType();
                    boolean isUrgent = "bill".equalsIgnoreCase(type) || "announcement".equalsIgnoreCase(type);

                    // Skip tray alert ONLY for non-urgent items if user is already on the notifications screen
                    if (!isUrgent && com.upreyvan.carti.utils.AppLifecycleTracker.isNotificationsActive()) return;

                    // If user is in chat, ignore social/chat alerts, but ALWAYS show Bill/Announcement alerts
                    if (!isUrgent && com.upreyvan.carti.utils.AppLifecycleTracker.isChatActive()) return;

                    // Messenger-style: Only notify if status is unread
                    if (!n.isUnread()) return;

                    boolean isChatNotif = "chat".equalsIgnoreCase(type) || "social".equalsIgnoreCase(type);
                    // Show system notification
                    com.upreyvan.carti.utils.NotificationHelper.showNotification(
                        realtimeRepo.getContext(), 
                        n.getTitle(), 
                        n.getContent(),
                        isChatNotif
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
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("isRead", true);
        
        updateNotification(notificationId, data, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                synchronized (currentNotifications) {
                    for (Notification n : currentNotifications) {
                        if (n.getId().equals(notificationId)) {
                            n.setRead(true);
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

    public void markAsPaid(String notificationId) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("status", "PAID");
        data.put("isRead", true);
        
        updateNotification(notificationId, data, null);
    }

    public void markBillAsPaidByCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return;
        
        synchronized (currentNotifications) {
            for (Notification n : currentNotifications) {
                String title = n.getTitle();
                if (title == null) continue;
                
                String upperTitle = title.toUpperCase();
                // Check if it's a bill and matches the category name
                boolean isBill = upperTitle.startsWith("BILL:") || upperTitle.startsWith("DUE TODAY:") || 
                                upperTitle.startsWith("DUE TOMORROW:") || upperTitle.startsWith("OVERDUE:");
                
                if (isBill && title.toLowerCase().contains(categoryName.toLowerCase())) {
                    if (!"PAID".equalsIgnoreCase(n.getStatus())) {
                        markAsPaid(n.getId());
                    }
                }
            }
        }
    }

    public void markAllAsRead() {
        apiHelper.markAllNotificationsRead(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                synchronized (currentNotifications) {
                    for (Notification n : currentNotifications) {
                        n.setRead(true);
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
        // Use gateway (Appwrite Function) for security as requested
        apiHelper.updateNotification(id, data, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                refreshNotifications();
                refreshBills(pref.getFamilyId());
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void refreshBills(String familyId) {
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
            @Override public void onError(Throwable error) {}
        });
    }

    public LiveData<List<Bill>> getBills(String familyId) {
        if (billsLiveData.getValue() == null || !familyId.equals(lastFamilyId)) {
            lastFamilyId = familyId;
            refreshBills(familyId);
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
        
        Object isReadObj = data.get("isRead");
        if (isReadObj instanceof Boolean) n.setRead((Boolean) isReadObj);
        else if (isReadObj instanceof String) n.setRead(Boolean.parseBoolean((String) isReadObj));

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
        if (title == null) return null;

        String upperTitle = title.toUpperCase();
        boolean isBill = upperTitle.startsWith("BILL: ") || 
                        upperTitle.startsWith("OVERDUE: ") || 
                        upperTitle.startsWith("DUE TODAY: ") || 
                        upperTitle.startsWith("DUE TOMORROW: ");

        if (isBill) {
            String name = title.substring(title.indexOf(":") + 1).trim();
            String content = (String) data.get("content");
            String date = "";
            
            // Try to extract date from content or timestamp
            if (content != null && content.toLowerCase().contains("due on ")) {
                int index = content.toLowerCase().lastIndexOf("due on ");
                String rawDatePart = content.substring(index + 7).trim();
                if (rawDatePart.contains(".")) {
                    date = rawDatePart.substring(0, rawDatePart.indexOf(".")).trim();
                } else if (rawDatePart.contains("\n")) {
                    date = rawDatePart.substring(0, rawDatePart.indexOf("\n")).trim();
                } else {
                    date = rawDatePart;
                }
            }
            
            // Fallback to timestamp if date extraction failed
            if (date.isEmpty()) {
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

            // Check database status first
            String dbStatus = (String) data.get("status");
            String statusLabel = "Upcoming";
            int iconRes = R.drawable.ic_calendar;

            if ("PAID".equalsIgnoreCase(dbStatus)) {
                statusLabel = "Paid";
            } else if (!date.isEmpty()) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                    java.util.Date dueDate = sdf.parse(date);
                    if (dueDate != null) {
                        java.util.Calendar now = java.util.Calendar.getInstance();
                        now.set(java.util.Calendar.HOUR_OF_DAY, 0);
                        now.set(java.util.Calendar.MINUTE, 0);
                        now.set(java.util.Calendar.SECOND, 0);
                        now.set(java.util.Calendar.MILLISECOND, 0);

                        java.util.Calendar due = java.util.Calendar.getInstance();
                        due.setTime(dueDate);
                        due.set(java.util.Calendar.HOUR_OF_DAY, 0);
                        due.set(java.util.Calendar.MINUTE, 0);
                        due.set(java.util.Calendar.SECOND, 0);
                        due.set(java.util.Calendar.MILLISECOND, 0);

                        if (due.before(now)) {
                            statusLabel = "Overdue";
                        } else if (due.equals(now)) {
                            statusLabel = "Due Today";
                            iconRes = R.drawable.ic_bell;
                        } else {
                            java.util.Calendar tomorrow = (java.util.Calendar) now.clone();
                            tomorrow.add(java.util.Calendar.DAY_OF_YEAR, 1);
                            if (due.equals(tomorrow)) {
                                statusLabel = "Due Tomorrow";
                                iconRes = R.drawable.ic_bell;
                            }
                        }

                        // Auto-update server if status changed and not yet updated
                        checkAndAutoUpdateBillStatus(id, title, content, statusLabel, name);
                    }
                } catch (Exception ignored) {}
            }

            return new Bill(
                    id,
                    (String) data.get("familyId"),
                    name,
                    date,
                    statusLabel,
                    iconRes,
                    amount,
                    category
            );
        }
        return null;
    }

    private void checkAndAutoUpdateBillStatus(String id, String title, String content, String status, String name) {
        boolean isOverdue = "Overdue".equals(status);
        boolean isDueToday = "Due Today".equals(status);
        boolean isDueTomorrow = "Due Tomorrow".equals(status);
        
        if (!isOverdue && !isDueToday && !isDueTomorrow) return;

        String prefix = isOverdue ? "OVERDUE: " : (isDueToday ? "DUE TODAY: " : "DUE TOMORROW: ");
        
        // Check if title already has the correct prefix to avoid redundant updates
        if (title.startsWith(prefix)) return;

        // Messenger-style check: Only update server once per transition per device 
        String prefKey = "notified_" + id + "_" + status;
        if (pref.getContext().getSharedPreferences("BillNotifs", Context.MODE_PRIVATE).getBoolean(prefKey, false)) return;

        String icon = isOverdue ? "⚠️ " : "🔔 ";
        String newTitle = prefix + name;
        
        // Generate descriptive content based on status
        String descriptiveSentence;
        if (isOverdue) {
            descriptiveSentence = "This bill for " + name + " is now OVERDUE. Please settle it to avoid extra charges.";
        } else if (isDueToday) {
            descriptiveSentence = "Friendly reminder: Your " + name + " bill is due TODAY. Don't forget to pay!";
        } else {
            descriptiveSentence = "Heads up! Your " + name + " bill is due TOMORROW. Please prepare your payment.";
        }

        // Keep any existing notes from the original content
        String existingNotes = "";
        if (content != null) {
            if (content.contains("Notes: ")) {
                existingNotes = content.substring(content.indexOf("Notes: "));
            } else if (content.contains("Details: ")) {
                existingNotes = content.substring(content.indexOf("Details: "));
            }
        }

        String newContent = icon + descriptiveSentence + (existingNotes.isEmpty() ? "" : "\n\n" + existingNotes);
        
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("title", newTitle);
        update.put("content", newContent);
        update.put("isRead", false); 
        update.put("status", isOverdue ? "OVERDUE" : (isDueToday ? "DUE_TODAY" : "DUE_TOMORROW"));

        updateNotification(id, update, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> result) {
                pref.getContext().getSharedPreferences("BillNotifs", Context.MODE_PRIVATE)
                    .edit().putBoolean(prefKey, true).apply();
            }
            @Override public void onError(Throwable error) {}
        });
    }
}
