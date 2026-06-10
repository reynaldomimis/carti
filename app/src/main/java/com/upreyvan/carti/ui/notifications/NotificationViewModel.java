package com.upreyvan.carti.ui.notifications;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.repository.FamilyRepository;
import com.upreyvan.carti.repository.NotificationRepository;
import com.upreyvan.carti.models.Notification;
import java.util.List;
import java.util.Map;

public class NotificationViewModel extends BaseViewModel {
    private final NotificationRepository notificationRepo;
    private final FamilyRepository familyRepo;
    private final MutableLiveData<String> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        this.notificationRepo = NotificationRepository.getInstance(application);
        this.familyRepo = FamilyRepository.getInstance(application);
    }

    public LiveData<List<Notification>> getNotifications() {
        return notificationRepo.getNotifications();
    }

    public LiveData<String> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getError() { return error; }

    public void loadAll() {
        notificationRepo.refreshNotifications();
    }

    public void markAsRead(String id) {
        notificationRepo.markAsRead(id);
    }

    public void markAllAsRead() {
        notificationRepo.markAllAsRead();
    }

    public void approveMember(String userId) {
        setLoading(true);
        familyRepo.approveJoinRequest(userId, new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
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
        familyRepo.rejectJoinRequest(userId, new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
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
