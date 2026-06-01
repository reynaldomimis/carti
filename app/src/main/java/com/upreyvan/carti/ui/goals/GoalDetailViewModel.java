package com.upreyvan.carti.ui.goals;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.List;

public class GoalDetailViewModel extends AndroidViewModel {
    private final TransactionRepository repository;

    public GoalDetailViewModel(@NonNull Application application) {
        super(application);
        repository = TransactionRepository.getInstance(application);
    }

    public LiveData<Transaction> getGoal(String goalId) {
        return Transformations.map(repository.getGoals(), goals -> {
            if (goals != null) {
                for (TransactionWithUser t : goals) {
                    if (t.getTransaction().getId().equals(goalId)) return t.getTransaction();
                }
            }
            return null;
        });
    }

    public LiveData<List<TransactionWithUser>> getHistory(String goalId) {
        return repository.getTransactionsByAllocation(goalId);
    }
}
