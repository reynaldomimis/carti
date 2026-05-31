package com.upreyvan.carti.data.repository;

import android.content.Context;
import com.upreyvan.carti.data.ai.AiActionHandler;
import com.upreyvan.carti.data.ai.AiManager;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.ChatMessage;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AiRepository {
    private final AiManager aiManager;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final BudgetManager budgetManager;
    private final AiActionHandler actionHandler;
    private final com.upreyvan.carti.data.local.db.dao.TransactionDao transactionDao;
    private long lastCallTime = 0;
    private static final long RATE_LIMIT_MS = 3000;
    private static AiRepository instance;

    public static AiRepository getInstance(Context context) {
        if (instance == null) instance = new AiRepository(context);
        return instance;
    }

    public AiRepository(Context context) {
        this.aiManager = AiManager.getInstance(context);
        this.apiHelper = new ApiHelper(context);
        this.pref = new PreferenceManager(context);
        this.budgetManager = BudgetManager.getInstance(context);
        this.actionHandler = new AiActionHandler(context);
        this.transactionDao = com.upreyvan.carti.data.local.db.AppDatabase.getInstance(context).transactionDao();
    }

    public void processChat(String msg, String sender, List<ChatMessage> history, boolean force, AiManager.AiCallback cb) {
        long now = System.currentTimeMillis();
        if (!force && (now - lastCallTime < RATE_LIMIT_MS || msg.length() < 2 || msg.toLowerCase().matches("^(ok|okay|salamat|thanks|bye|ty|k|tnx|haha|hehe)$"))) return;
        lastCallTime = now;
        apiHelper.getTransactionsSince("2024-01-01T00:00:00.000Z", new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override public void onSuccess(DocumentList<Map<String, Object>> r) { callAi(msg, buildContext(r.getDocuments(), history), force, cb); }
            @Override public void onError(Throwable e) { callAi(msg, "B:" + pref.getBalance(), force, cb); }
        });
    }

    private void callAi(String msg, String ctx, boolean f, AiManager.AiCallback cb) {
        String fullCtx = ctx + "\nINTRO_DONE: " + (pref.isAiIntroDone() ? "TRUE" : "FALSE");
        aiManager.processChat(msg, fullCtx, f, new AiManager.AiCallback() {
            @Override public void onSuccess(String r) { 
                pref.setAiIntroDone(true);
                cb.onSuccess(r); 
            }
            @Override public void onError(Throwable t) { cb.onError(t); }
            @Override public void onActionDetected(JSONObject a) { 
                pref.setAiIntroDone(true);
                cb.onActionDetected(a); 
            }
        });
    }

    public void executeAction(JSONObject action) {
        actionHandler.executeAction(action);
    }

    private String buildContext(List<Document<Map<String, Object>>> docs, List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "B:%.0f|I:%.0f|E:%.0f\n", pref.getBalance(), pref.getTotalIncome(), pref.getTotalExpense()));

        if (docs != null && !docs.isEmpty()) { 
            sb.append("TX:"); 
            int c = 0; 
            for (Document<Map<String, Object>> d : docs) { 
                if (c++ >= 3) break; 
                Map<String, Object> m = d.getData(); 
                sb.append(m.get("amount")).append(",").append(m.get("category")).append(";"); 
            } 
        }

        if (history != null && !history.isEmpty()) { 
            sb.append("\nH:"); 
            int start = Math.max(0, history.size() - 5); 
            for (int i = start; i < history.size(); i++) {
                ChatMessage m = history.get(i);
                if (!m.isShimmer()) {
                    sb.append(m.getSenderName()).append(":").append(m.getMessage()).append("|");
                }
            }
        }
        return sb.toString();
    }

    public void getDailyInsights(AiManager.AiCallback cb) {
        new Thread(() -> { try { List<com.upreyvan.carti.model.Transaction> txs = transactionDao.getAllTransactionsList(pref.getFamilyId()); aiManager.getInsights(buildContextFromRoom(txs), new AiManager.AiCallback() {
            @Override public void onSuccess(String r) { cb.onSuccess(r); }
            @Override public void onError(Throwable t) { cb.onError(t); }
        }); } catch (Exception e) { cb.onError(e); } }).start();
    }

    public void getSmartSuggestions(AiManager.AiCallback callback) {
        Calendar cal = Calendar.getInstance();
        String weekKey = cal.get(Calendar.WEEK_OF_YEAR) + "-" + cal.get(Calendar.YEAR);
        new Thread(() -> {
            try {
                List<com.upreyvan.carti.model.Transaction> txs = transactionDao.getAllTransactionsList(pref.getFamilyId());
                String ctx = buildContextFromRoom(txs);
                aiManager.generateResponse("TASK: generate 4 coaching suggestions as JSON Array [{\"title\":\"\",\"description\":\"\",\"type\":\"SAVINGS|EXPENSE|GOAL|BILL\",\"actionText\":\"\"}]. CONTEXT:\n" + ctx, new AiManager.AiCallback() {
                    @Override public void onSuccess(String r) { pref.saveDailyAiSuggestions(weekKey, r); callback.onSuccess(r); }
                    @Override public void onError(Throwable t) { callback.onError(t); }
                });
            } catch (Exception e) { callback.onError(e); }
        }).start();
    }

    private String buildContextFromRoom(List<com.upreyvan.carti.model.Transaction> txs) {
        return String.format(Locale.US, "B:%.0f|I:%.0f|E:%.0f", pref.getBalance(), pref.getTotalIncome(), pref.getTotalExpense());
    }
}
