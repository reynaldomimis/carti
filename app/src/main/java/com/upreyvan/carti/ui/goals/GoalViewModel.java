package com.upreyvan.carti.ui.goals;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionType;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.utils.Constants;
import java.io.File;
import java.util.List;

public class GoalViewModel extends BaseViewModel {
    private final TransactionRepository repository;
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public GoalViewModel(@NonNull Application application) {
        super(application);
        this.repository = TransactionRepository.getInstance(application);
    }

    public LiveData<List<TransactionWithUser>> getGoals() { return repository.getGoals(); }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getError() { return error; }

    public void saveGoal(String name, double targetAmount, double savedAmount, String date, int iconRes, Uri imageUri) {
        setLoading(true);
        if (imageUri != null) {
            uploadAndSave(name, targetAmount, savedAmount, date, iconRes, imageUri);
        } else {
            performSave(name, targetAmount, savedAmount, date, iconRes, null);
        }
    }

    private void uploadAndSave(String name, double targetAmount, double savedAmount, String date, int iconRes, Uri imageUri) {
        File file = new File(imageUri.getPath());
        repository.uploadIcon(file, new AppwriteCallback<io.appwrite.models.File>() {
            @Override
            public void onSuccess(io.appwrite.models.File result) {
                String fileUrl = String.format("%s/storage/buckets/%s/files/%s/view?project=%s",
                        com.upreyvan.carti.BuildConfig.APPWRITE_ENDPOINT,
                        Constants.Appwrite.BUCKET_ICONS,
                        result.getId(),
                        com.upreyvan.carti.BuildConfig.APPWRITE_PROJECT_ID);
                performSave(name, targetAmount, savedAmount, date, 0, fileUrl);
            }

            @Override
            public void onError(Throwable e) {
                setLoading(false);
                error.postValue("Upload failed: " + e.getMessage());
            }
        });
    }

    private void performSave(String name, double targetAmount, double savedAmount, String date, int iconRes, String iconUrl) {
        Transaction t = new Transaction();
        t.setTitle(name);
        t.setCategory(name);
        t.setTargetAmount(targetAmount);
        t.setAmount(savedAmount);
        t.setTargetDate(date);
        t.setIconRes(iconRes);
        t.setIconUrl(iconUrl);
        t.setStatus("ACTIVE");
        
        // MAANGAS MOVE: Generate a UUID now and use it as the 'allocatedTo' for the parent too.
        // This way, the parent and all its contributions share the exact same tag.
        String goalTag = "G-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        t.setAllocatedTo(goalTag);

        repository.createItem(TransactionType.GOAL, t, loadingCallback(() -> saveSuccess.postValue(true), error));
    }
}
