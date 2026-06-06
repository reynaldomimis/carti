package com.upreyvan.carti.ui.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.AuthRepository;
import io.appwrite.models.Session;
import java.util.Map;

public class AuthViewModel extends BaseViewModel {
    private final AuthRepository repository;
    private final PreferenceManager pref;
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.repository = AuthRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
    }

    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<Boolean> getRegisterSuccess() { return registerSuccess; }
    public LiveData<String> getError() { return error; }

    public void login(String email, String password) {
        setLoading(true);
        repository.login(email, password, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Session result) {
                pref.setUserId(result.getUserId());
                fetchUserDetails();
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void register(String email, String password, String username, boolean isEmployed, String role) {
        setLoading(true);
        repository.register(email, password, username, isEmployed, role, new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                login(email, password);
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void updatePasswordRecovery(String userId, String secret, String newPassword) {
        setLoading(true);
        repository.updatePasswordRecovery(userId, secret, newPassword, new AppwriteCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                setLoading(false);
                loginSuccess.postValue(true);
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void consumeLoginSuccess() {
        loginSuccess.setValue(false);
    }

    private void fetchUserDetails() {
        repository.getUser(new AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> r) {
                pref.saveUser(r);
                setLoading(false);
                loginSuccess.postValue(true);
            }
            @Override public void onError(Throwable e) {
                setLoading(false);
                String msg = e.getMessage();
                if (msg != null && (msg.contains("401") || msg.contains("Unauthorized"))) {
                    error.postValue(msg);
                } else {
                    loginSuccess.postValue(true); 
                }
            }
        });
    }
}
