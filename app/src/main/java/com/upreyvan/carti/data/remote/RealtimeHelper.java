package com.upreyvan.carti.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.ToastHelper;

import java.util.Map;

import io.appwrite.models.RealtimeResponseEvent;
import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class RealtimeHelper {

    private final Realtime realtime;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private final Context context;

    public RealtimeHelper(Context context) {
        this.context = context.getApplicationContext();
        realtime = new Realtime(AppwriteManager.getInstance(context).getClient());
    }

    public interface RealtimeEventCallback {
        void onEvent(RealtimeResponseEvent<?> event);
        default void onError(Throwable error) {
            Log.e("RealtimeHelper", "Event Error: " + (error != null ? error.getMessage() : "Unknown"));
        }
    }

    /**
     * Subscribes to all events in a collection.
     */
    public RealtimeSubscription subscribeToCollection(String collectionId, RealtimeEventCallback callback) {
        String channel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents";
        return subscribe(new String[]{channel}, callback);
    }

    /**
     * Subscribes to events for a specific document.
     */
    public RealtimeSubscription subscribeToDocument(String collectionId, String documentId, RealtimeEventCallback callback) {
        String channel = "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents." + documentId;
        return subscribe(new String[]{channel}, callback);
    }

    /**
     * Generic subscription for multiple channels.
     */
    public RealtimeSubscription subscribe(String[] channels, RealtimeEventCallback callback) {
        try {
            return realtime.subscribe(channels, event -> {
                mainHandler.post(() -> callback.onEvent(event));
                return null;
            });
        } catch (Exception e) {
            Log.e("RealtimeHelper", "Subscription error", e);
            mainHandler.post(() -> {
                callback.onError(e);
                ToastHelper.show(context, Constants.ErrorCodes.NETWORK_ERROR, ToastHelper.Status.ERROR);
            });
            return null;
        }
    }

    public static boolean isCreateEvent(RealtimeResponseEvent<?> event) {
        for (Object e : event.getEvents()) {
            if (String.valueOf(e).endsWith(".create")) return true;
        }
        return false;
    }

    public static boolean isUpdateEvent(RealtimeResponseEvent<?> event) {
        for (Object e : event.getEvents()) {
            if (String.valueOf(e).endsWith(".update")) return true;
        }
        return false;
    }

    public static boolean isDeleteEvent(RealtimeResponseEvent<?> event) {
        for (Object e : event.getEvents()) {
            if (String.valueOf(e).endsWith(".delete")) return true;
        }
        return false;
    }

    /**
     * Extracts the payload as a Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getPayload(RealtimeResponseEvent<?> event) {
        return (Map<String, Object>) event.getPayload();
    }

    /**
     * Parses the event payload into a specific model class.
     */
    public <T> T parsePayload(RealtimeResponseEvent<?> event, Class<T> clazz) {
        try {
            String json = gson.toJson(event.getPayload());
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            Log.e("RealtimeHelper", "Error parsing realtime payload", e);
            return null;
        }
    }
}
