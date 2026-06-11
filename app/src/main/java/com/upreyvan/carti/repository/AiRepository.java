package com.upreyvan.carti.repository;

import android.content.Context;
import com.upreyvan.carti.datasource.ApiHelper;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.managers.ai.AiActionHandler;
import com.upreyvan.carti.models.ChatMessage;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;

/**
 * Phase 4: Server-side AI Repository.
 * Routes all AI requests to Appwrite Functions to protect API keys.
 */
public class AiRepository {
    private static AiRepository instance;
    private final ApiHelper apiHelper;
    private final AiActionHandler actionHandler;
    private final com.upreyvan.carti.managers.PreferenceManager pref;
    private static final long INSIGHT_COOLDOWN = 3600000; // 1 Hour

    private AiRepository(Context context) {
        this.apiHelper = new ApiHelper(context);
        this.actionHandler = new AiActionHandler(context);
        this.pref = com.upreyvan.carti.managers.PreferenceManager.getInstance(context);
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
        // Optimized: Always attach the latest centralized context if not provided
        String finalContext = context;
        if (finalContext == null || finalContext.isEmpty()) {
            com.upreyvan.carti.models.FinancialSummary fs = TransactionRepository.getInstance(pref.getContext()).getFinancialSummary().getValue();
            if (fs != null) {
                finalContext = String.format("Balance: ₱%.2f, Spent: ₱%.2f, Saved: ₱%.2f", 
                    fs.monthlyBalance(), fs.monthlyExpense(), fs.totalAccumulatedSavings());
            }
        }

        apiHelper.getChatAi(message, finalContext, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (result != null) {
                    if (result.containsKey("action")) {
                        try {
                            Object actionObj = result.get("action");
                            if (actionObj instanceof Map) {
                                callback.onActionDetected(new JSONObject((Map<?, ?>) actionObj));
                            } else if (actionObj instanceof String) {
                                callback.onActionDetected(new JSONObject((String) actionObj));
                            }
                        } catch (Exception e) {
                            android.util.Log.e("AiRepository", "Action parse error", e);
                        }
                    }
                    
                    if (result.containsKey("response")) {
                        callback.onSuccess(String.valueOf(result.get("response")));
                    } else {
                        callback.onError(new Exception("No AI response text"));
                    }
                } else {
                    callback.onError(new Exception("Invalid AI response"));
                }
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void executeAction(JSONObject action) {
        actionHandler.executeAction(action);
    }

    public void getInsights(String context, AiCallback callback) {
        // Check Cache first
        String cachedInsight = pref.getAiInsightsCache();
        long lastFetch = pref.getAiInsightsTimestamp();
        long currentTime = System.currentTimeMillis();

        if (!cachedInsight.isEmpty() && (currentTime - lastFetch < INSIGHT_COOLDOWN)) {
            callback.onSuccess(cachedInsight);
            return;
        }

        // Optimized: Use passed context or fallback to summary
        String finalContext = context;
        if (finalContext == null || finalContext.isEmpty()) {
            com.upreyvan.carti.models.FinancialSummary fs = TransactionRepository.getInstance(pref.getContext()).getFinancialSummary().getValue();
            if (fs != null) {
                finalContext = String.format("Summary: Balance ₱%.2f, Expense ₱%.2f", fs.monthlyBalance(), fs.monthlyExpense());
            }
        }

        apiHelper.getAiInsights(finalContext, new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (result != null && result.containsKey("insight")) {
                    String insight = String.valueOf(result.get("insight"));
                    pref.setAiInsightsCache(insight);
                    callback.onSuccess(insight);
                } else {
                    callback.onError(new Exception("Invalid AI insight response"));
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!cachedInsight.isEmpty()) {
                    callback.onSuccess(cachedInsight); 
                } else {
                    callback.onError(error);
                }
            }
        });
    }
}
