package com.upreyvan.carti.data.repository;

import android.content.Context;
import com.upreyvan.carti.data.ai.AiActionHandler;
import com.upreyvan.carti.data.ai.GeminiManager;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.ChatMessage;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ELITE SENIOR LEVEL: Optimized Performance Repository.
 */
public class AiRepository {

    private final GeminiManager geminiManager;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final BudgetManager budgetManager;
    private final AiActionHandler actionHandler;
    
    private long lastCallTime = 0;
    private static final long RATE_LIMIT_MS = 2000;

    public AiRepository(Context context) {
        this.geminiManager = GeminiManager.getInstance(context);
        this.apiHelper = new ApiHelper(context);
        this.pref = new PreferenceManager(context);
        this.budgetManager = BudgetManager.getInstance(context);
        this.actionHandler = new AiActionHandler(context);
    }

    public void processChat(String message, String senderName, List<ChatMessage> chatHistory, boolean isForce, GeminiManager.AiCallback callback) {
        // LAYER 1: Immediate Exit for spam
        long now = System.currentTimeMillis();
        if (!isForce && now - lastCallTime < RATE_LIMIT_MS) return;
        lastCallTime = now;

        // LAYER 2: Context Necessity Check (Is it financial?)
        String msgLower = message.toLowerCase();
        boolean needsDb = msgLower.matches(".*[₱$0-9].*|.*(gastos|balance|income|report|history|log|expense).*");

        // Intro check
        boolean introFound = false;
        if (chatHistory != null && !chatHistory.isEmpty()) {
            for (ChatMessage m : chatHistory) {
                if (m.getSenderName() != null && m.getSenderName().toLowerCase().contains("carti")) {
                    introFound = true;
                    break;
                }
            }
        }
        String extraContext = "family_id:" + pref.getFamilyId() + "|INTRO_DONE:" + introFound;

        if (!needsDb) {
            callGemini(message, "CASUAL_MODE: " + pref.getUsername() + "|" + extraContext, isForce, callback);
            return;
        }

        apiHelper.getTransactionsSince("2024-01-01T00:00:00.000Z", new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                callGemini(message, buildCompressedContext(result.getDocuments(), chatHistory) + "|" + extraContext, isForce, callback);
            }

            @Override
            public void onError(Throwable error) {
                callGemini(message, "B:" + pref.getBalance() + "|" + extraContext, isForce, callback);
            }
        });
    }

    private void callGemini(String message, String context, boolean force, GeminiManager.AiCallback uiCallback) {
        geminiManager.processChat(message, context, force, new GeminiManager.AiCallback() {
            @Override
            public void onSuccess(String response) { uiCallback.onSuccess(response); }
            @Override
            public void onError(Throwable t) { uiCallback.onError(t); }
            @Override
            public void onActionDetected(JSONObject action) {
                actionHandler.executeAction(action);
                uiCallback.onActionDetected(action);
            }
        });
    }

    private String buildCompressedContext(List<Document<Map<String, Object>>> docs, List<ChatMessage> chatHistory) {
        double income = pref.getTotalIncome();
        double expense = pref.getTotalExpense();
        double savings = income - expense;
        double balance = pref.getBalance();

        Map<String, Double> breakdown = new java.util.HashMap<>();
        if (docs != null) {
            for (Document<Map<String, Object>> doc : docs) {
                Map<String, Object> d = doc.getData();
                String type = String.valueOf(d.get("type"));
                if ("EXPENSE".equalsIgnoreCase(type)) {
                    String cat = String.valueOf(d.get("category"));
                    double amt = com.upreyvan.carti.util.Utils.getDouble(d.get("amount"));
                    breakdown.put(cat, breakdown.getOrDefault(cat, 0.0) + amt);
                }
            }
        }

        String topCategory = "None";
        double maxAmt = 0;
        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            if (entry.getValue() > maxAmt) {
                maxAmt = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("family_id:").append(pref.getFamilyId()).append("\n");
        sb.append(String.format(Locale.US, "FINANCIAL_SUMMARY:\nIncome:%.0f|Expense:%.0f|Savings:%.0f|Balance:%.0f\n", 
            income, expense, savings, balance));
        sb.append("Top Spending: ").append(topCategory).append("\n");
        
        if (!breakdown.isEmpty()) {
            sb.append("Breakdown: ");
            for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
                sb.append(entry.getKey()).append(":").append(String.format(Locale.US, "%.0f", entry.getValue())).append(";");
            }
            sb.append("\n");
        }

        // Include Budget Plan summary for coaching context
        List<BudgetCategoryItem> plan = budgetManager.getBudgetPlan();
        if (plan != null && !plan.isEmpty()) {
            sb.append("PLAN:");
            for (BudgetCategoryItem item : plan) {
                sb.append(item.getCategoryName()).append(":").append(String.format(Locale.US, "%.0f", item.getAmount())).append(";");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }


    public void getDailyInsights(GeminiManager.AiCallback callback) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        String cached = pref.getDailyAiInsightText();
        String cachedDate = pref.getDailyAiInsightDate();

        if (today.equals(cachedDate) && !cached.isEmpty()) {
            callback.onSuccess(cached);
            return;
        }

        apiHelper.getTransactionsSince("2024-01-01T00:00:00.000Z", new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                geminiManager.getInsights(buildCompressedContext(result.getDocuments(), null), new GeminiManager.AiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        pref.saveDailyAiInsight(today, response);
                        callback.onSuccess(response);
                    }
                    @Override
                    public void onError(Throwable t) { callback.onError(t); }
                });
            }
            @Override
            public void onError(Throwable error) { callback.onError(error); }
        });
    }

}
