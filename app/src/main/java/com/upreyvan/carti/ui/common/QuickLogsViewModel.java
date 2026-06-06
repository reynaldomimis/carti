package com.upreyvan.carti.ui.common;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.TransactionHandler;
import java.util.List;
import java.util.stream.Collectors;

public class QuickLogsViewModel extends BaseViewModel {
    private final TransactionRepository transRepo;
    private final MemberRepository memberRepo;
    private final MutableLiveData<String> category = new MutableLiveData<>();
    private final LiveData<Double> remainingBalance;
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public QuickLogsViewModel(@NonNull Application application) {
        super(application);
        this.transRepo = TransactionRepository.getInstance(application);
        this.memberRepo = MemberRepository.getInstance(application);
        
        this.remainingBalance = Transformations.switchMap(category, cat -> 
            Transformations.map(transRepo.getAllTransactions(), list -> {
                double allocated = 5000.0; // Same as hardcoded in legacy helper
                double spent = list.stream()
                    .filter(tu -> "EXPENSE".equalsIgnoreCase(tu.getTransaction().getType()) && cat.equalsIgnoreCase(tu.getTransaction().getCategory()))
                    .mapToDouble(tu -> tu.getTransaction().getAmount())
                    .sum();
                return allocated - spent;
            })
        );
    }

    public void setCategory(String cat) {
        category.setValue(cat);
    }

    public LiveData<Double> getRemainingBalance() { return remainingBalance; }
    public LiveData<List<Member>> getMembers() { return memberRepo.getMembers(); }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getError() { return error; }

    public void saveTrack(double amount, String cat, String description, String source) {
        setLoading(true);
        TransactionHandler.saveTrack(getApplication(), amount, cat, description, source, new TransactionHandler.TransactionCallback() {
            @Override public void onLoading(boolean isLoading) {}
            @Override public void onSuccess(Transaction t) { setLoading(false); saveSuccess.postValue(true); }
            @Override public void onError(String message) { setLoading(false); error.postValue(message); }
        });
    }

    public void saveDebt(double amount, String person, String description) {
        setLoading(true);
        TransactionHandler.saveDebt(getApplication(), amount, person, description, new TransactionHandler.TransactionCallback() {
            @Override public void onLoading(boolean isLoading) {}
            @Override public void onSuccess(Transaction t) { setLoading(false); saveSuccess.postValue(true); }
            @Override public void onError(String message) { setLoading(false); error.postValue(message); }
        });
    }

    public void saveGoal(double amount, String cat) {
        setLoading(true);
        TransactionHandler.saveGoal(getApplication(), amount, cat, new TransactionHandler.TransactionCallback() {
            @Override public void onLoading(boolean isLoading) {}
            @Override public void onSuccess(Transaction t) { setLoading(false); saveSuccess.postValue(true); }
            @Override public void onError(String message) { setLoading(false); error.postValue(message); }
        });
    }
}
