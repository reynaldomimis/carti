package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Bill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Document;

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

    public LiveData<List<Bill>> getBills(String familyId) {
        MediatorLiveData<List<Bill>> billsLiveData = new MediatorLiveData<>();
        List<Bill> currentBills = new ArrayList<>();

        // Initial fetch
        new ApiHelper(realtimeRepo.getContext()).getNotifications(familyId, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    Bill bill = mapToBill(doc.getId(), doc.getData());
                    if (bill != null) currentBills.add(bill);
                }
                billsLiveData.postValue(new ArrayList<>(currentBills));
            }

            @Override
            public void onError(Throwable error) {}
        });

        // Observe realtime stream
        billsLiveData.addSource(realtimeRepo.getNotificationStream(), payload -> {
            if (payload != null && familyId.equals(payload.get("familyId"))) {
                String id = (String) payload.get("$id");
                Bill bill = mapToBill(id, payload);
                if (bill != null) {
                    // Update or Add
                    boolean found = false;
                    for (int i = 0; i < currentBills.size(); i++) {
                        if (currentBills.get(i).getId().equals(id)) {
                            currentBills.set(i, bill);
                            found = true;
                            break;
                        }
                    }
                    if (!found) currentBills.add(0, bill);
                    billsLiveData.setValue(new ArrayList<>(currentBills));
                }
            }
        });

        return billsLiveData;
    }

    private Bill mapToBill(String id, Map<String, Object> data) {
        String title = (String) data.get("title");
        if (title != null && title.startsWith("BILL: ")) {
            String name = title.substring(6);
            String content = (String) data.get("content");
            String date = "";
            if (content != null && content.contains("due on ")) {
                date = content.substring(content.lastIndexOf("due on ") + 7);
            }
            return new Bill(
                    id,
                    (String) data.get("familyId"),
                    name,
                    date,
                    "Upcoming",
                    R.drawable.ic_calendar
            );
        }
        return null;
    }
}
