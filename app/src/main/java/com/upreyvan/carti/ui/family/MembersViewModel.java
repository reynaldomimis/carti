package com.upreyvan.carti.ui.family;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.model.Member;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.appwrite.models.User;

public class MembersViewModel extends BaseViewModel {
    private final MemberRepository repository;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>();

    public MembersViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MemberRepository(application);
        this.pref = PreferenceManager.getInstance(application);
        this.currentUserId.setValue(pref.getUserId());
        refreshBudget();
    }

    public LiveData<List<Member>> getMembers() {
        return repository.getMembers();
    }

    public LiveData<String> getCurrentUserId() {
        return currentUserId;
    }

    public LiveData<Double> getTotalIncome() { return totalIncome; }
    public LiveData<Double> getTotalExpense() { return totalExpense; }

    public void refreshData() {
        repository.syncMembersIfNeeded();
        fetchCurrentUser();
        refreshBudget();
    }

    private void refreshBudget() {
        executor.execute(() -> {
            double income = pref.getTotalIncome();
            double expense = pref.getTotalExpense();
            totalIncome.postValue(income);
            totalExpense.postValue(expense);
        });
    }

    private void fetchCurrentUser() {
        AppwriteManager.getInstance(getApplication()).getCurrentUser(new AppwriteManager.AppwriteCallback<User<Map<String, Object>>>() {
            @Override
            public void onSuccess(User<Map<String, Object>> result) {
                currentUserId.postValue(result.getId());
            }

            @Override
            public void onError(Throwable error) {
                currentUserId.postValue(pref.getUserId());
            }
        });
    }
}
