package com.upreyvan.carti.ui.onboarding;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.OnboardingRepository;
import java.util.Map;

public class OnboardingViewModel extends BaseViewModel {
    private final OnboardingRepository repository;
    private final PreferenceManager pref;
    private final MutableLiveData<Map<String, Object>> userStatus = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> familyCreated = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> joinRequested = new MutableLiveData<>(false);

    public OnboardingViewModel(@NonNull Application application) {
        super(application);
        this.repository = OnboardingRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
    }

    public LiveData<Map<String, Object>> getUserStatus() { return userStatus; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getFamilyCreated() { return familyCreated; }
    public LiveData<Boolean> getJoinRequested() { return joinRequested; }

    public void fetchUserStatus() {
        setLoading(true);
        repository.getUserStatus(new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                pref.saveUser(result);
                userStatus.postValue(result);
                setLoading(false);
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void createFamily(String name) {
        setLoading(true);
        repository.createFamily(name, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                fetchUserStatus();
                familyCreated.postValue(true);
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void joinFamily(String inviteCode) {
        setLoading(true);
        repository.joinFamily(inviteCode, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                fetchUserStatus();
                joinRequested.postValue(true);
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
