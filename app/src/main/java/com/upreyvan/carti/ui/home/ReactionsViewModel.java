package com.upreyvan.carti.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.Reactor;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReactionsViewModel extends BaseViewModel {
    private final TransactionRepository repository;
    private final MutableLiveData<List<Reactor>> reactors = new MutableLiveData<>(new ArrayList<>());

    public ReactionsViewModel(@NonNull Application application) {
        super(application);
        this.repository = TransactionRepository.getInstance(application);
    }

    public LiveData<List<Reactor>> getReactors() { return reactors; }

    public void loadReactions(String transactionId) {
        setLoading(true);
        // Using ApiHelper via repository (need to add to repo if not there)
        // For now, assume it's in TransactionRepository or similar
        // Let's add it to TransactionRepository for consistency
        repository.getLikes(transactionId, new AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                List<Reactor> list = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    String username = String.valueOf(data.get("username"));
                    String emoji = String.valueOf(data.get("emojiType"));
                    String userId = String.valueOf(data.get("userId"));
                    list.add(new Reactor(username, emoji, 0, null, userId));
                }
                reactors.postValue(list);
                setLoading(false);
            }
            @Override public void onError(Throwable error) {
                setLoading(false);
            }
        });
    }
}
