package com.upreyvan.carti.data.ai;

import android.content.Context;
import android.util.Log;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.upreyvan.carti.BuildConfig;
import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiManager {

    private static volatile GeminiManager instance;
    private final Client client;
    private final Executor executor;

    private static final String ELITE_SYSTEM_PROMPT = 
        "IDENTITY: Carti AI, smart financial coach. " +
        "RULES: 1. STRICTLY ENGLISH ONLY. 2. EXTREMELY CONCISE. 3. Use PHP (₱). " +
        "STYLE: Be a proactive coach. Summarize everything. No long paragraphs. Use bullet points if needed. " +
        
        "CONVERSATIONAL RULES: " +
        "1. WARM UP: If INTRO_DONE:TRUE, JUST say 'Yes, how can I help you?' or something very short. DO NOT re-introduce yourself. If FALSE, greet warmly as Carti coach. " +
        "2. LOGGING: If an action like ADD_EXPENSE/ADD_INCOME is detected, skip greetings entirely. Just confirm the transaction detail immediately. No 'Hello' or 'I am Carti' here. " +
        "3. SCOPE LOCK: If non-financial, respond: 'I am sorry, I only track family budgets. How can I help with that?' " +
        
        "ACTION ENGINE: For financial logs, start with JSON: {\"action\": \"...\", \"data\": {...}} " +
        "Example: {\"action\": \"ADD_EXPENSE\", \"data\": {...}} Recorded ₱100 for food.";

    private GeminiManager(Context context) {
        this.client = Client.builder()
                .apiKey(BuildConfig.GEMINI_API_KEY)
                .build();
        this.executor = Executors.newFixedThreadPool(4);
    }

    public static GeminiManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GeminiManager.class) {
                if (instance == null) {
                    instance = new GeminiManager(context);
                }
            }
        }
        return instance;
    }

    public interface AiCallback {
        void onSuccess(String response);
        void onError(Throwable t);
        default void onActionDetected(JSONObject action) {}
    }

    public void generateResponse(String prompt, AiCallback callback) {
        executor.execute(() -> {
            try {
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-3.5-flash",
                        prompt,
                        null
                );

                String resultText = response.text();
                if (resultText == null || resultText.trim().isEmpty()) {
                    return; 
                }

                if (resultText.contains("[IGNORE]")) {
                    Log.d("GeminiManager", "AI status: SILENT");
                    return;
                }

                // ELITE PARSING: Extract JSON and send ONLY the text part to UI
                if (resultText.trim().startsWith("{")) {
                    try {
                        int lastBrace = resultText.lastIndexOf("}") + 1;
                        String jsonPart = resultText.substring(0, lastBrace);
                        String messagePart = resultText.substring(lastBrace).trim();
                        
                        JSONObject actionJson = new JSONObject(jsonPart);
                        callback.onActionDetected(actionJson);
                        
                        // Show only text to user. If AI forgot text, use default.
                        if (!messagePart.isEmpty()) {
                            callback.onSuccess(messagePart);
                        } else {
                            callback.onSuccess("Transaction recorded successfully.");
                        }
                    } catch (Exception e) {
                        callback.onSuccess(resultText);
                    }
                } else {
                    callback.onSuccess(resultText);
                }
            } catch (Exception e) {
                Log.e("GeminiManager", "SDK Error: " + e.getMessage());
                callback.onError(e);
            }
        });
    }

    public void processChat(String message, String context, boolean force, AiCallback callback) {
        String mode = force ? "DIRECT_COMMAND" : "PASSIVE_LISTENING";
        String finalPrompt = ELITE_SYSTEM_PROMPT + "\n\nMODE: " + mode + 
                "\n\n[CONTEXT]:\n" + context + 
                "\n\n[USER]: " + message;
        generateResponse(finalPrompt, callback);
    }

    public void getInsights(String context, AiCallback callback) {
        String prompt = ELITE_SYSTEM_PROMPT + "\n\nTASK: Generate financial health insights based on these logs. " +
                "Be proactive. Identify leaks or potential savings.\n\n[CONTEXT]:\n" + context;
        generateResponse(prompt, callback);
    }
}
