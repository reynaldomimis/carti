package com.upreyvan.carti.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.AppwriteManager;
import java.util.Map;

public class MainViewModel extends AndroidViewModel {
    private final PreferenceManager pref;
    private final com.upreyvan.carti.repository.AuthRepository authRepo;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        pref = PreferenceManager.getInstance(application);
        authRepo = com.upreyvan.carti.repository.AuthRepository.getInstance(application);
    }

    public LiveData<AuthState> getAuthState() { return authState; }

    public void validateGate() {
        if (authState.getValue() == AuthState.AUTHENTICATED) return;

        if (pref.getUserId().isEmpty()) {
            authState.postValue(AuthState.UNAUTHENTICATED);
            return;
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (authState.getValue() == null || authState.getValue() == AuthState.ERROR) {
                if (!pref.getFamilyId().isEmpty()) {
                    authState.postValue(AuthState.AUTHENTICATED);
                } else if (!pref.getUserId().isEmpty()) {
                    authState.postValue(AuthState.NO_FAMILY);
                } else {
                    authState.postValue(AuthState.UNAUTHENTICATED);
                }
            }
        }, 10000);

        authRepo.getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> r) {
                pref.saveUser(r);
                String familyId = (r.get("familyId") != null && !"null".equals(String.valueOf(r.get("familyId")))) ? String.valueOf(r.get("familyId")) : "";
                String pendingId = (r.get("pendingFamilyId") != null && !"null".equals(String.valueOf(r.get("pendingFamilyId")))) ? String.valueOf(r.get("pendingFamilyId")) : "";

                if (!familyId.isEmpty()) {
                    authState.postValue(AuthState.AUTHENTICATED);
                } else if ("declined".equals(pendingId)) {
                    authState.postValue(AuthState.DECLINED);
                } else if (!pendingId.isEmpty()) {
                    authState.postValue(AuthState.PENDING);
                } else {
                    authState.postValue(AuthState.NO_FAMILY);
                }
            }

            @Override
            public void onError(Throwable e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("401") || msg.contains("Unauthorized") || msg.contains(com.upreyvan.carti.utils.Constants.ErrorCodes.UNAUTHORIZED))) {
                    authState.postValue(AuthState.UNAUTHENTICATED);
                } else {
                    if (!pref.getFamilyId().isEmpty()) {
                        authState.postValue(AuthState.AUTHENTICATED);
                    } else if (!pref.getUserId().isEmpty()) {
                        authState.postValue(AuthState.NO_FAMILY);
                    } else {
                        authState.postValue(AuthState.UNAUTHENTICATED);
                    }
                }
            }
        });
    }

    public void forceLogout() {
        pref.clear();
        authState.postValue(AuthState.UNAUTHENTICATED);
    }

    public enum AuthState {
        AUTHENTICATED, UNAUTHENTICATED, PENDING, DECLINED, NO_FAMILY, ERROR
    }
}
