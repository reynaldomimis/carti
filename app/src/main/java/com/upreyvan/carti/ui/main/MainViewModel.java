package com.upreyvan.carti.ui.main;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import java.util.Map;

public class MainViewModel extends AndroidViewModel {
    private final PreferenceManager pref;
    private final ApiHelper apiHelper;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        pref = new PreferenceManager(application);
        apiHelper = new ApiHelper(application);
    }

    public LiveData<AuthState> getAuthState() { return authState; }

    public void validateGate() {
        if (pref.getUserId().isEmpty()) {
            authState.setValue(AuthState.UNAUTHENTICATED);
            return;
        }


        if (!pref.getFamilyId().isEmpty()) {
            authState.postValue(AuthState.AUTHENTICATED);
        }

        apiHelper.getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> r) {
                String fid = (r.get("familyId") != null && !"null".equals(String.valueOf(r.get("familyId")))) ? String.valueOf(r.get("familyId")) : "";
                if (!fid.isEmpty()) {
                    pref.setFamilyId(fid);
                    if (pref.getFamilyId().isEmpty()) authState.postValue(AuthState.AUTHENTICATED);
                } else {
                    authState.postValue(AuthState.NO_FAMILY);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!pref.getFamilyId().isEmpty()) {
                    authState.postValue(AuthState.AUTHENTICATED);
                } else {
                    if (e.getMessage() != null && e.getMessage().contains("401")) {
                        authState.postValue(AuthState.UNAUTHENTICATED);
                    } else {
                        authState.postValue(AuthState.ERROR);
                    }
                }
            }
        });
    }

    public void forceLogout() {
        new Thread(() -> {
            try { AppDatabase.getInstance(getApplication()).clearAllTables(); } catch (Exception ignored) {}
            pref.clear();
            authState.postValue(AuthState.UNAUTHENTICATED);
        }).start();
    }

    public enum AuthState {
        AUTHENTICATED, UNAUTHENTICATED, NO_FAMILY, ERROR
    }
}
