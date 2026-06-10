package com.upreyvan.carti.realtime;

import android.content.Context;
import com.google.gson.Gson;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.utils.Constants;
import java.util.Map;
import io.appwrite.models.RealtimeResponseEvent;
import io.appwrite.models.RealtimeSubscription;
import io.appwrite.services.Realtime;

public class RealtimeHelper {
    private final Realtime realtime;
    private final Gson gson = new Gson();

    public RealtimeHelper(Context context) {
        realtime = new Realtime(AppwriteManager.getInstance(context).getClient());
    }

    public interface RealtimeEventCallback {
        void onEvent(RealtimeResponseEvent<?> event);
    }

    public RealtimeSubscription subscribe(String[] channels, RealtimeEventCallback callback) {
        try {
            return realtime.subscribe(channels, event -> {
                callback.onEvent(event);
                return null;
            });
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isCreateEvent(RealtimeResponseEvent<?> event) {
        return hasEventSuffix(event, ".create");
    }

    public static boolean isUpdateEvent(RealtimeResponseEvent<?> event) {
        return hasEventSuffix(event, ".update");
    }

    public static boolean isDeleteEvent(RealtimeResponseEvent<?> event) {
        return hasEventSuffix(event, ".delete");
    }

    private static boolean hasEventSuffix(RealtimeResponseEvent<?> event, String suffix) {
        if (event == null || event.getEvents() == null) return false;
        for (String e : event.getEvents()) {
            if (e.endsWith(suffix)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getPayload(RealtimeResponseEvent<?> event) {
        if (event == null) return null;
        return (Map<String, Object>) event.getPayload();
    }

    public <T> T parsePayload(RealtimeResponseEvent<?> event, Class<T> clazz) {
        if (event == null || event.getPayload() == null) return null;
        try {
            String json = gson.toJson(event.getPayload());
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCollectionChannel(String collectionId) {
        return "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents";
    }

    public static String getDocumentChannel(String collectionId, String documentId) {
        return "databases." + Constants.Appwrite.DATABASE_ID + ".collections." + collectionId + ".documents." + documentId;
    }
}
