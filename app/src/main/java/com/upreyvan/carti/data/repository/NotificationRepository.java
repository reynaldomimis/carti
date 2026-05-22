package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.util.Constants;
import java.util.Map;

/**
 * Senior Architecture: Centralized Realtime Data Hub.
 * Minimizes bandwidth by delivering full payloads directly to observers, 
 * eliminating the need for redundant "refresh" API calls.
 */
public class NotificationRepository {

    private static NotificationRepository instance;
    private final RealtimeRepository realtimeRepo;

    private NotificationRepository(Context context) {
        this.realtimeRepo = RealtimeRepository.getInstance(context);
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
}
