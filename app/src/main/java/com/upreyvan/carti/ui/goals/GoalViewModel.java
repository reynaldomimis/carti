package com.upreyvan.carti.ui.goals;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.List;

public class GoalViewModel extends AndroidViewModel {
    private final TransactionRepository repository;

    public GoalViewModel(@NonNull Application application) {
        super(application);
        repository = TransactionRepository.getInstance(application);
    }

    public LiveData<List<TransactionWithUser>> getGoals() {
        return repository.getGoals();
    }

    public void refresh() {
        repository.syncTransactionsIfNeeded();
        repository.refreshTransactions();
    }

    public void deleteGoal(String goalId) {
        repository.deleteTransaction(goalId, null);
    }
}
