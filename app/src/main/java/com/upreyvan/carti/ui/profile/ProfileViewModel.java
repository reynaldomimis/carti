package com.upreyvan.carti.ui.profile;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.ProfileRepository;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.FinancialSummary;
import java.util.Map;

public class ProfileViewModel extends BaseViewModel {
    private final ProfileRepository repository;
    private final TransactionRepository transRepo;
    private final PreferenceManager pref;
    private final MutableLiveData<Integer> memberCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.repository = ProfileRepository.getInstance(application);
        this.transRepo = TransactionRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
    }

    public LiveData<Integer> getMemberCount() { return memberCount; }
    public LiveData<Boolean> getLogoutSuccess() { return logoutSuccess; }
    public LiveData<String> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getError() { return error; }

    /**
     * Future-ready: Monthly Income stats from Central Engine.
     */
    public LiveData<Double> getMonthlyIncome() {
        return Transformations.map(transRepo.getFinancialSummary(), FinancialSummary::monthlyIncome);
    }

    /**
     * Future-ready: Monthly Savings stats (Income - Expense) from Central Engine.
     */
    public LiveData<Double> getMonthlySavings() {
        return Transformations.map(transRepo.getFinancialSummary(), FinancialSummary::monthlySavings);
    }

    public void fetchMemberCount() {
        repository.getMembers(new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<io.appwrite.models.DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(io.appwrite.models.DocumentList<Map<String, Object>> result) {
                memberCount.postValue(result.getDocuments().size());
            }
            @Override public void onError(Throwable e) { memberCount.postValue(0); }
        });
    }

    public void logout() {
        setLoading(true);
        repository.logout(new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Object>() {
            @Override public void onSuccess(Object result) { setLoading(false); logoutSuccess.postValue(true); }
            @Override public void onError(Throwable e) { setLoading(false); error.postValue(e.getMessage()); }
        });
    }

    public void logoutAll() {
        setLoading(true);
        repository.logoutAll(new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Object>() {
            @Override public void onSuccess(Object result) { setLoading(false); logoutSuccess.postValue(true); }
            @Override public void onError(Throwable e) { setLoading(false); error.postValue(e.getMessage()); }
        });
    }

    public void updatePassword(String old, String next) {
        setLoading(true);
        repository.updatePassword(old, next, new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<io.appwrite.models.User<Map<String, Object>>>() {
            @Override public void onSuccess(io.appwrite.models.User<Map<String, Object>> result) { 
                setLoading(false); 
                actionSuccess.postValue("Password updated successfully!");
            }
            @Override public void onError(Throwable e) { 
                setLoading(false); 
                error.postValue(e.getMessage());
            }
        });
    }

    public void deleteAccount() {
        setLoading(true);
        repository.deleteAccount(new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) { setLoading(false); logoutSuccess.postValue(true); }
            @Override public void onError(Throwable e) { setLoading(false); }
        });
    }
}
