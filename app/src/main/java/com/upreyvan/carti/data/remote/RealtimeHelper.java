package com.upreyvan.carti.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.upreyvan.carti.util.Constants;

import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class RealtimeHelper {

    private final Realtime realtime;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RealtimeHelper(Context context) {
        realtime = new Realtime(AppwriteManager.getInstance(context).getClient());
    }

    public interface RealtimeCallback {
        void onUpdate();
    }

    public RealtimeSubscription subscribeToCollection(String collectionId, RealtimeCallback callback) {
        String channel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents";
        return realtime.subscribe(new String[]{channel}, event -> {
            mainHandler.post(callback::onUpdate);
            return null;
        });
    }

    public RealtimeSubscription subscribeToDocument(String collectionId, String documentId, RealtimeCallback callback) {
        String channel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents." + documentId;
        return realtime.subscribe(new String[]{channel}, event -> {
            mainHandler.post(callback::onUpdate);
            return null;
        });
    }
}
