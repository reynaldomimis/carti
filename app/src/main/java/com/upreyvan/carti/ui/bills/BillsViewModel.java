package com.upreyvan.carti.ui.bills;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.repository.NotificationRepository;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.TransactionType;
import java.util.List;
import java.util.Map;

public class BillsViewModel extends BaseViewModel {
    private final NotificationRepository repository;
    private final PreferenceManager pref;
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);

    public BillsViewModel(@NonNull Application application) {
        super(application);
        this.repository = NotificationRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
    }

    public LiveData<List<Bill>> getBills() {
        return repository.getBills(pref.getFamilyId());
    }

    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }

    public void saveBill(String title, String content, double amount, String category) {
        setLoading(true);
        repository.sendAnnouncement(title, content, amount, category, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                setLoading(false);
                saveSuccess.postValue(true);
            }
            @Override public void onError(Throwable error) {
                setLoading(false);
            }
        });
    }

    public void updateBill(String id, String title, String content, double amount, String category) {
        setLoading(true);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("title", title);
        data.put("content", content);
        data.put("amount", amount);
        data.put("category", category);
        
        repository.updateNotification(id, data, new AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                setLoading(false);
                saveSuccess.postValue(true);
            }
            @Override public void onError(Throwable error) {
                setLoading(false);
            }
        });
    }

    public void payBill(String id, String name, double amount, String notes) {
        setLoading(true);
        TransactionRepository.getInstance(getApplication()).createItem(TransactionType.EXPENSE, amount, "Bills", name + ": " + notes, new AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                // Bridge Logic: Mark as 'paid' and seen (isRead=true)
                Map<String, Object> update = new java.util.HashMap<>();
                update.put("status", "PAID");
                update.put("isRead", true);

                repository.updateNotification(id, update, new AppwriteCallback<Map<String, Object>>() {
                    @Override public void onSuccess(Map<String, Object> result) {
                        repository.refreshBills(pref.getFamilyId());
                        setLoading(false);
                        saveSuccess.postValue(true);
                    }
                    @Override public void onError(Throwable error) {
                        setLoading(false);
                        saveSuccess.postValue(true);
                    }
                });
            }
            @Override public void onError(Throwable error) {
                setLoading(false);
            }
        });
    }

    public void deleteBill(String id) {
        setLoading(true);
        repository.deleteNotification(id, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Object result) {
                setLoading(false);
                saveSuccess.postValue(true);
            }
            @Override public void onError(Throwable error) {
                setLoading(false);
            }
        });
    }
}
