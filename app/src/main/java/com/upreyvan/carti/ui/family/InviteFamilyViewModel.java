package com.upreyvan.carti.ui.family;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.repository.FamilyRepository;
import io.appwrite.models.Document;
import java.util.Map;

public class InviteFamilyViewModel extends BaseViewModel {
    private final FamilyRepository repository;
    private final PreferenceManager pref;
    private final MutableLiveData<String> inviteCode = new MutableLiveData<>();

    public InviteFamilyViewModel(@NonNull Application application) {
        super(application);
        this.repository = FamilyRepository.getInstance(application);
        this.pref = PreferenceManager.getInstance(application);
        this.inviteCode.setValue(pref.getInviteCode());
    }

    public LiveData<String> getInviteCode() { return inviteCode; }

    public void fetchInviteCode() {
        repository.getFamilySummary(new AppwriteCallback<>() {
            @Override
            public void onSuccess(Document<Map<String, Object>> result) {
                if (result.getData() != null) {
                    Object code = result.getData().get("inviteCode");
                    if (code != null) {
                        String codeStr = String.valueOf(code);
                        pref.setInviteCode(codeStr);
                        inviteCode.postValue(codeStr);
                    }
                }
            }
            @Override public void onError(Throwable error) {}
        });
    }
}
