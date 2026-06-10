package com.upreyvan.carti.base;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.upreyvan.carti.datasource.AppwriteManager;

public abstract class BaseViewModel extends AndroidViewModel {
    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    protected final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    protected void setError(String message) { errorMessage.postValue(message); }
    protected void setLoading(boolean loading) { isLoading.postValue(loading); }

    protected <T> AppwriteManager.AppwriteCallback<T> loadingCallback(Runnable onSuccess) {
        return loadingCallback(onSuccess, errorMessage);
    }

    protected <T> AppwriteManager.AppwriteCallback<T> loadingCallback(Runnable onSuccess, MutableLiveData<String> errorTarget) {
        return new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(T result) {
                setLoading(false);
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                MutableLiveData<String> target = errorTarget != null ? errorTarget : errorMessage;
                target.postValue(error.getMessage());
            }
        };
    }
}
