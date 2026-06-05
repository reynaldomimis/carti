package com.upreyvan.carti.ui.family;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.lifecycle.MediatorLiveData;
import io.appwrite.models.User;

public class MembersViewModel extends BaseViewModel {
    private final MemberRepository repository;
    private final TransactionDao transactionDao;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>();
    private final MediatorLiveData<List<Member>> membersWithContributions = new MediatorLiveData<>();

    public MembersViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MemberRepository(application);
        this.transactionDao = AppDatabase.getInstance(application).transactionDao();
        this.pref = PreferenceManager.getInstance(application);
        this.currentUserId.setValue(pref.getUserId());
        
        setupMembersMediator();
        refreshBudget();
    }

    private void setupMembersMediator() {
        Calendar cal = Calendar.getInstance();
        long start = Utils.getMonthStartMillis(cal);
        long end = Utils.getMonthEndMillis(cal);

        LiveData<List<Member>> membersSource = repository.getMembers();
        LiveData<List<TransactionDao.UserExpenseSum>> expenseSumsSource = transactionDao.getExpenseSumPerUser(pref.getFamilyId(), start, end);
        LiveData<List<TransactionDao.UserExpenseSum>> allocationSumsSource = transactionDao.getAllocationSumPerUser(pref.getFamilyId(), start, end);

        membersWithContributions.addSource(membersSource, members -> combine(members, expenseSumsSource.getValue(), allocationSumsSource.getValue()));
        membersWithContributions.addSource(expenseSumsSource, sums -> combine(membersSource.getValue(), sums, allocationSumsSource.getValue()));
        membersWithContributions.addSource(allocationSumsSource, sums -> combine(membersSource.getValue(), expenseSumsSource.getValue(), sums));
    }

    private void combine(List<Member> members, List<TransactionDao.UserExpenseSum> expenseSums, List<TransactionDao.UserExpenseSum> allocationSums) {
        if (members == null) return;
        
        Map<String, Double> expMap = new HashMap<>();
        if (expenseSums != null) {
            for (TransactionDao.UserExpenseSum s : expenseSums) expMap.put(s.userId, s.total);
        }

        Map<String, Double> allocMap = new HashMap<>();
        if (allocationSums != null) {
            for (TransactionDao.UserExpenseSum s : allocationSums) allocMap.put(s.userId, s.total);
        }

        List<Member> updatedMembers = new ArrayList<>();
        for (Member m : members) {
            Double allocObj = allocMap.get(m.getId());
            double allocation = allocObj != null ? allocObj : 0.0;

            Double expObj = expMap.get(m.getId());
            double expense = expObj != null ? expObj : 0.0;

            Member updated = new Member(m.getId(), m.getFamilyId(), m.getTitle(), m.getDescription(), m.getStatus(), m.getAvatarRes(), allocation);
            updated.setAvatarUrl(m.getAvatarUrl());
            updated.setTotalExpense(expense);
            updatedMembers.add(updated);
        }
        membersWithContributions.setValue(updatedMembers);
    }

    public LiveData<List<Member>> getMembers() {
        return membersWithContributions;
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
