package com.upreyvan.carti.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.util.Constants;

import java.util.Map;

import io.appwrite.models.Document;

/**
 * Senior Level Repository for Chat.
 * Handles both REST API (via ApiHelper) and Realtime updates.
 */
public class ChatRepository {

    private final ApiHelper apiHelper;
    private final RealtimeRepository realtimeRepo;

    public ChatRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
        this.realtimeRepo = RealtimeRepository.getInstance(context);
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
    public LiveData<Map<String, Object>> getRealtimeMessages() {
        return realtimeRepo.getChatStream();
    }

    /**
     * Clean up resources. Should be called when the ViewModel is cleared.
     */
    public void onDestroy() {
        // No-op as the hub is central
    }
}
