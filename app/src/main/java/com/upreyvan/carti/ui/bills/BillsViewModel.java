package com.upreyvan.carti.ui.bills;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.NotificationRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Bill;
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

    public void saveBill(String title, String content) {
        setLoading(true);
        repository.sendAnnouncement(title, content, new AppwriteCallback<>() {
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
        TransactionRepository.getInstance(getApplication()).addTransaction(amount, "EXPENSE", "Bills", name + ": " + notes, new AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                deleteBill(id);
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
