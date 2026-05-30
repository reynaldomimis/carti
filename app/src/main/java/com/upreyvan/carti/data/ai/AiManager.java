package com.upreyvan.carti.data.ai;

import android.content.Context;
import org.json.JSONObject;

public class AiManager {
    private static volatile AiManager instance;
    private final AiOrchestrator orchestrator;
    private static final String PROMPT = "ID: Carti AI. Smart financial engine. STRICT ENGLISH. CONCISE. Use ₱. RULES: 1. GREET: Warmly if first turn. If repeated 'Hi', say 'Yes, how can I help?'. 2. LOCK: For non-finance, say: 'Family Tracker assistant only. Zipper mouth muna.' 3. INSIGHTS: Provide 1-sentence tips. 4. ACTIONS: Start with JSON {\"action\": \"...\", \"data\": {...}} then short text.";

    private AiManager(Context context) { this.orchestrator = AiOrchestrator.getInstance(context); }

    public static AiManager getInstance(Context context) {
        if (instance == null) { synchronized (AiManager.class) { if (instance == null) instance = new AiManager(context); } }
        return instance;
    }

    public interface AiCallback { void onSuccess(String response); void onError(Throwable t); default void onActionDetected(JSONObject action) {} }

    public void generateResponse(String prompt, AiCallback callback) {
        orchestrator.request(prompt, new AiOrchestrator.AiGatewayCallback() {
            @Override public void onSuccess(String res) { callback.onSuccess(res); }
            @Override public void onError(Throwable t) { callback.onError(t); }
            @Override public void onActionDetected(JSONObject act) { callback.onActionDetected(act); }
        });
    }

    public void processChat(String msg, String ctx, boolean force, AiCallback cb) { generateResponse(PROMPT + "\nMODE: " + (force ? "CMD" : "PASS") + "\nCTX: " + ctx + "\nUSER: " + msg, cb); }

    public void getInsights(String ctx, AiCallback cb) { generateResponse(PROMPT + "\nTASK: Budget report.\nCTX: " + ctx, cb); }
}
