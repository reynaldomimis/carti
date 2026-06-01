package com.upreyvan.carti.ui.debt;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.List;

public class DebtViewModel extends AndroidViewModel {
    private final TransactionRepository repository;

    public DebtViewModel(@NonNull Application application) {
        super(application);
        repository = TransactionRepository.getInstance(application);
    }

    public LiveData<List<TransactionWithUser>> getDebts() {
        return repository.getTransactionsByType("DEBT");
    }

    public void refresh() {
        repository.syncTransactionsIfNeeded();
        repository.refreshTransactions();
    }
}
