package com.upreyvan.carti.ui.debt;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.List;
import java.util.Map;

public class DebtViewModel extends BaseViewModel {
    private final TransactionRepository repository;
    private final MutableLiveData<Boolean> markPaidSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public DebtViewModel(@NonNull Application application) {
        super(application);
        repository = TransactionRepository.getInstance(application);
    }

    public LiveData<List<TransactionWithUser>> getDebts() {
        return repository.getTransactionsByType("DEBT");
    }

    public LiveData<Boolean> getMarkPaidSuccess() { return markPaidSuccess; }
    public LiveData<String> getError() { return error; }

    public void refresh() {
        repository.refreshTransactions();
    }

    public void markDebtPaid(String debtId) {
        setLoading(true);
        repository.markDebtPaid(debtId, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                setLoading(false);
                markPaidSuccess.postValue(true);
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
