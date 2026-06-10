package com.upreyvan.carti.ui.profile;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.repository.ProfileRepository;
import io.appwrite.models.DocumentList;
import io.appwrite.models.User;
import java.util.Map;

public class ProfileViewModel extends BaseViewModel {
    private final ProfileRepository repository;
    private final PreferenceManager pref;
    private final MutableLiveData<Integer> memberCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.repository = ProfileRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
    }

    public LiveData<Integer> getMemberCount() { return memberCount; }
    public LiveData<Boolean> getLogoutSuccess() { return logoutSuccess; }
    public LiveData<String> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getError() { return error; }

    public void fetchMemberCount() {
        repository.getMembers(new AppwriteCallback<>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (result.getDocuments() != null) {
                    memberCount.postValue(result.getDocuments().size());
                }
            }
            @Override public void onError(Throwable error) {}
        });
    }

    public void logout() {
        setLoading(true);
        repository.logout(new AppwriteCallback<>() {
            @Override
            public void onSuccess(Object result) {
                pref.clear();
                setLoading(false);
                logoutSuccess.postValue(true);
            }
            @Override public void onError(Throwable e) {
                pref.clear();
                setLoading(false);
                logoutSuccess.postValue(true); 
            }
        });
    }

    public void logoutAll() {
        setLoading(true);
        repository.logoutAll(new AppwriteCallback<>() {
            @Override
            public void onSuccess(Object result) {
                pref.clear();
                setLoading(false);
                logoutSuccess.postValue(true);
                actionSuccess.postValue("Signed out from all devices");
            }
            @Override public void onError(Throwable e) {
                pref.clear();
                setLoading(false);
                logoutSuccess.postValue(true);
            }
        });
    }

    public void updatePassword(String newPass, String oldPass) {
        setLoading(true);
        repository.updatePassword(newPass, oldPass, new AppwriteCallback<User<Map<String, Object>>>() {
            @Override
            public void onSuccess(User<Map<String, Object>> result) {
                setLoading(false);
                actionSuccess.postValue("Password updated successfully");
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void deleteAccount() {
        setLoading(true);
        repository.deleteAccount(new AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                pref.clear();
                setLoading(false);
                logoutSuccess.postValue(true);
                actionSuccess.postValue("Account deleted successfully");
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
