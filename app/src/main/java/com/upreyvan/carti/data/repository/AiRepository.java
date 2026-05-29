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
import java.util.Calendar;
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
    private final com.upreyvan.carti.data.local.db.dao.TransactionDao transactionDao;
    
    private long lastCallTime = 0;
    // ELITE SAFETY: Increased to 5000ms (12 RPM) to avoid Google's 15 RPM Free Tier limit
    private static final long RATE_LIMIT_MS = 5000;

    public AiRepository(Context context) {
        this.geminiManager = GeminiManager.getInstance(context);
        this.apiHelper = new ApiHelper(context);
        this.pref = new PreferenceManager(context);
        this.budgetManager = BudgetManager.getInstance(context);
        this.actionHandler = new AiActionHandler(context);
        this.transactionDao = com.upreyvan.carti.data.local.db.AppDatabase.getInstance(context).transactionDao();
    }

    public void processChat(String message, String senderName, List<ChatMessage> chatHistory, boolean isForce, GeminiManager.AiCallback callback) {
        // LAYER 1: Immediate Exit for spam
        long now = System.currentTimeMillis();
        if (!isForce && now - lastCallTime < RATE_LIMIT_MS) {
            callback.onSuccess(""); // Notify caller that we're skipping due to rate limit
            return;
        }
        lastCallTime = now;

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

        // ELITE FIX: Always fetch local context to make Carti "Smart" again
        new Thread(() -> {
            try {
                List<com.upreyvan.carti.model.Transaction> transactions = transactionDao.getAllTransactionsList(pref.getFamilyId());
                String context = buildCompressedContextFromRoom(transactions, chatHistory) + "|" + extraContext;
                
                // If it's a very simple greeting, we still use Gemini but with the context
                callGemini(message, context, isForce, callback);
            } catch (Exception e) {
                callGemini(message, "B:" + pref.getBalance() + "|" + extraContext, isForce, callback);
            }
        }).start();
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

    private String buildCompressedContextFromRoom(List<com.upreyvan.carti.model.Transaction> transactions, List<ChatMessage> chatHistory) {
        double income = pref.getTotalIncome();
        double expense = pref.getTotalExpense();
        double savings = income - expense;
        double balance = pref.getBalance();

        // ELITE OPTIMIZATION: Get pre-aggregated breakdown from SQL
        List<com.upreyvan.carti.data.local.db.dao.TransactionDao.CategorySum> breakdown = transactionDao.getExpenseBreakdown(pref.getFamilyId());

        StringBuilder sb = new StringBuilder();
        sb.append("family_id:").append(pref.getFamilyId()).append("\n");
        sb.append(String.format(Locale.US, "FINANCIAL_SUMMARY:\nIncome:%.0f|Expense:%.0f|Savings:%.0f|Balance:%.0f\n", 
            income, expense, savings, balance));

        if (breakdown != null && !breakdown.isEmpty()) {
            sb.append("Top Spending: ").append(breakdown.get(0).category).append("\n");
            sb.append("Breakdown: ");
            for (com.upreyvan.carti.data.local.db.dao.TransactionDao.CategorySum item : breakdown) {
                sb.append(item.category).append(":").append(String.format(Locale.US, "%.0f", item.total)).append(";");
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

        // TOKEN SAVER: Only include the very last message for minimal context
        if (chatHistory != null && !chatHistory.isEmpty()) {
            sb.append("CHAT_HISTORY:\n");
            int start = Math.max(0, chatHistory.size() - 1);
            for (int i = start; i < chatHistory.size(); i++) {
                ChatMessage m = chatHistory.get(i);
                sb.append(m.isMe() ? "User: " : "Carti: ").append(m.getMessage()).append("\n");
            }
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

        new Thread(() -> {
            try {
                List<com.upreyvan.carti.model.Transaction> transactions = transactionDao.getAllTransactionsList(pref.getFamilyId());
                String context = buildCompressedContextFromRoom(transactions, null);
                geminiManager.getInsights(context, new GeminiManager.AiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        pref.saveDailyAiInsight(today, response);
                        callback.onSuccess(response);
                    }
                    @Override
                    public void onError(Throwable t) { callback.onError(t); }
                });
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void getSmartSuggestions(GeminiManager.AiCallback callback) {
        Calendar cal = Calendar.getInstance();
        String weekKey = cal.get(Calendar.WEEK_OF_YEAR) + "-" + cal.get(Calendar.YEAR);
        
        String cachedJson = pref.getDailyAiSuggestionsJson();
        String cachedWeek = pref.getDailyAiSuggestionsDate();

        if (weekKey.equals(cachedWeek) && !cachedJson.isEmpty()) {
            callback.onSuccess(cachedJson);
            return;
        }

        new Thread(() -> {
            try {
                List<com.upreyvan.carti.model.Transaction> transactions = transactionDao.getAllTransactionsList(pref.getFamilyId());
                String context = buildCompressedContextFromRoom(transactions, null);
                String prompt = "TASK: Based on the financial context of the PAST 4 WEEKS, generate EXACTLY 4 actionable coaching suggestions. " +
                        "Format: JSON Array of objects with keys: title, description, type (SAVINGS|EXPENSE|GOAL|BILL), actionText. " +
                        "Constraints: Keep titles extremely short (max 2-3 words). " +
                        "Focus on trends and patterns observed in the data. " +
                        "OUTPUT ONLY THE JSON ARRAY.\n\n[CONTEXT]:\n" + context;

                // Use 3.5 Flash for more intelligent suggestions
                geminiManager.generateResponse(GeminiManager.MODEL_FLASH_3_5, prompt, new GeminiManager.AiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        pref.saveDailyAiSuggestions(weekKey, response);
                        callback.onSuccess(response);
                    }
                    @Override
                    public void onError(Throwable t) { callback.onError(t); }
                });
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

}
