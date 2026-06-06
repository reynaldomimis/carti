package com.upreyvan.carti.ui.family;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.model.TransactionWithUser;
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
    private final TransactionRepository transRepo;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>();
    private final MediatorLiveData<List<Member>> membersWithContributions = new MediatorLiveData<>();

    public MembersViewModel(@NonNull Application application) {
        super(application);
        this.repository = MemberRepository.getInstance(application);
        this.transRepo = TransactionRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
        this.currentUserId.setValue(pref.getUserId());
        
        setupMembersMediator();
        refreshBudget();
    }

    private void setupMembersMediator() {
        LiveData<List<Member>> membersSource = repository.getMembers();
        LiveData<List<TransactionWithUser>> transactionsSource = transRepo.getAllTransactions();

        membersWithContributions.addSource(membersSource, members -> combine(members, transactionsSource.getValue()));
        membersWithContributions.addSource(transactionsSource, transactions -> combine(membersSource.getValue(), transactions));
    }

    private void combine(List<Member> members, List<TransactionWithUser> transactions) {
        if (members == null) return;
        
        Calendar cal = Calendar.getInstance();
        long start = Utils.getMonthStartMillis(cal);
        long end = Utils.getMonthEndMillis(cal);

        Map<String, Double> expMap = new HashMap<>();
        Map<String, Double> allocMap = new HashMap<>();

        if (transactions != null) {
            for (TransactionWithUser tu : transactions) {
                long t = tu.getTransaction().getTimestampMillis();
                if (t >= start && t <= end) {
                    String userId = tu.getTransaction().getUserId();
                    double amount = tu.getTransaction().getAmount();
                    if ("EXPENSE".equalsIgnoreCase(tu.getTransaction().getType())) {
                        expMap.put(userId, expMap.getOrDefault(userId, 0.0) + amount);
                    } else if ("ALLOCATION".equalsIgnoreCase(tu.getTransaction().getType())) {
                        allocMap.put(userId, allocMap.getOrDefault(userId, 0.0) + amount);
                    }
                }
            }
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
        repository.refreshMembers();
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

    public void approveMember(String userId) {
        com.upreyvan.carti.data.repository.FamilyRepository.getInstance(getApplication()).approveJoinRequest(userId, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) { refreshData(); }
            @Override public void onError(Throwable error) {}
        });
    }

    public void rejectMember(String userId) {
        com.upreyvan.carti.data.repository.FamilyRepository.getInstance(getApplication()).rejectJoinRequest(userId, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) { refreshData(); }
            @Override public void onError(Throwable error) {}
        });
    }

    private void fetchCurrentUser() {
        com.upreyvan.carti.data.repository.AuthRepository.getInstance(getApplication()).getCurrentUser(new AppwriteManager.AppwriteCallback<User<Map<String, Object>>>() {
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
