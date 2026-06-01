package com.upreyvan.carti.data.repository;

import android.content.Context;
import com.upreyvan.carti.data.ai.AiActionHandler;
import com.upreyvan.carti.data.ai.AiManager;
import com.upreyvan.carti.model.ChatMessage;
import org.json.JSONObject;
import java.util.List;

public class AiRepository {
    private static AiRepository instance;
    private final AiManager aiManager;
    private final AiActionHandler actionHandler;

    private AiRepository(Context context) {
        this.aiManager = AiManager.getInstance(context);
        this.actionHandler = new AiActionHandler(context);
    }

    public static synchronized AiRepository getInstance(Context context) {
        if (instance == null) instance = new AiRepository(context);
        return instance;
    }

    public interface AiCallback {
        void onSuccess(String response);
        void onError(Throwable t);
        void onActionDetected(JSONObject action);
    }

    public void processChat(String message, String context, List<ChatMessage> history, boolean isRealtime, AiCallback callback) {
        aiManager.processChat(message, context, new AiManager.AiCallback() {
            @Override public void onSuccess(String response) { callback.onSuccess(response); }
            @Override public void onError(Throwable t) { callback.onError(t); }
            @Override public void onActionDetected(JSONObject action) { callback.onActionDetected(action); }
        });
    }

    public void executeAction(JSONObject action) {
        actionHandler.executeAction(action);
    }
}
