package com.upreyvan.carti.ui.notifications;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.FamilyRepository;
import com.upreyvan.carti.data.repository.NotificationRepository;
import com.upreyvan.carti.model.Notification;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationViewModel extends BaseViewModel {
    private final NotificationRepository notificationRepo;
    private final FamilyRepository familyRepo;
    private final PreferenceManager pref;
    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        this.notificationRepo = NotificationRepository.getInstance(application);
        this.familyRepo = FamilyRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
    }

    public LiveData<List<Notification>> getNotifications() { return notifications; }
    public LiveData<String> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getError() { return error; }

    public void loadAll() {
        setLoading(true);
        List<Notification> list = new ArrayList<>();
        list.add(new Notification("Welcome to Carti!", "Start tracking your family expenses and reach your goals together.", System.currentTimeMillis(), Notification.Type.INFO, null));

        if (pref.isAdmin()) {
            familyRepo.getPendingMembers(new AppwriteCallback<DocumentList<Map<String, Object>>>() {
                @Override
                public void onSuccess(DocumentList<Map<String, Object>> result) {
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        String userName = String.valueOf(doc.getData().get("username"));
                        list.add(new Notification("New Join Request", userName + " wants to join your family.", System.currentTimeMillis(), Notification.Type.JOIN_REQUEST, doc.getId()));
                    }
                    loadAnnouncements(list);
                }
                @Override public void onError(Throwable e) { loadAnnouncements(list); }
            });
        } else {
            loadAnnouncements(list);
        }
    }

    private void loadAnnouncements(List<Notification> list) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) {
            notifications.postValue(list);
            setLoading(false);
            return;
        }

        notificationRepo.listNotifications(familyId, new AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    String title = String.valueOf(doc.getData().get("title"));
                    String content = String.valueOf(doc.getData().get("content"));
                    long ts = 0;
                    try { ts = com.upreyvan.carti.util.Utils.getMillisFromIso(String.valueOf(doc.getData().get("$createdAt"))); } catch (Exception ignored) {}
                    list.add(new Notification(title, content, ts > 0 ? ts : System.currentTimeMillis(), Notification.Type.INFO, null));
                }
                notifications.postValue(list);
                setLoading(false);
            }
            @Override public void onError(Throwable e) {
                notifications.postValue(list);
                setLoading(false);
            }
        });
    }

    public void approveMember(String userId) {
        setLoading(true);
        familyRepo.approveJoinRequest(userId, new AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) {
                actionSuccess.postValue("Member approved successfully!");
                loadAll();
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void rejectMember(String userId) {
        setLoading(true);
        familyRepo.rejectJoinRequest(userId, new AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) {
                actionSuccess.postValue("Join request rejected.");
                loadAll();
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
