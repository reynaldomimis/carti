package com.upreyvan.carti.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.util.Constants;

import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.models.RealtimeSubscription;

/**
 * Senior Level Repository for Chat.
 * Handles both REST API (via ApiHelper) and Realtime updates.
 */
public class ChatRepository {

    private final ApiHelper apiHelper;
    private final RealtimeHelper realtimeHelper;
    private RealtimeSubscription chatSubscription;

    public ChatRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
        this.realtimeHelper = new RealtimeHelper(context);
    }

    /**
     * Sends a message via ApiHelper.
     */
    public void sendMessage(String text, AppwriteManager.AppwriteCallback<Document<Map<String, Object>>> callback) {
        apiHelper.sendMessage(text, callback);
    }

    /**
     * Subscribes to realtime chat messages and returns a LiveData.
     */
    public LiveData<ChatMessage> getRealtimeMessages() {
        MutableLiveData<ChatMessage> liveData = new MutableLiveData<>();

        // Close previous subscription if any
        if (chatSubscription != null) {
            chatSubscription.close();
        }

        chatSubscription = realtimeHelper.subscribeToCollection(
                Constants.Appwrite.COL_MESSAGES,
                event -> {
                    if (RealtimeHelper.isCreateEvent(event)) {
                        ChatMessage msg = realtimeHelper.parsePayload(event, ChatMessage.class);
                        if (msg != null) {
                            liveData.setValue(msg);
                        }
                    }
                }
        );

        return liveData;
    }

    /**
     * Clean up resources. Should be called when the ViewModel is cleared.
     */
    public void onDestroy() {
        if (chatSubscription != null) {
            chatSubscription.close();
        }
    }
}
