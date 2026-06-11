package com.upreyvan.carti.managers.ai;

import android.content.Context;
import android.util.Log;
import com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionType;
import com.upreyvan.carti.utils.CategoryMapper;
import com.upreyvan.carti.utils.Utils;
import org.json.JSONObject;
import java.util.Map;

public class AiActionHandler {
    private final TransactionRepository transactionRepository;
    private final Context context;

    public AiActionHandler(Context context) {
        this.context = context;
        this.transactionRepository = TransactionRepository.getInstance(context);
    }

    public void executeAction(JSONObject json) {
        executeAction(json, null);
    }

    public void executeAction(JSONObject json, AppwriteCallback<Map<String, Object>> customCallback) {
        try {
            if (json == null) return;
            String rawIntent = json.optString("intent", json.optString("action", "EXPENSE"));
            String intent = normalizeToStandardType(rawIntent);
            
            JSONObject data = json.has("data") ? json.optJSONObject("data") : json;
            if (data == null) data = json;

            AppwriteCallback<Map<String, Object>> callback = (customCallback != null) ? customCallback : new SimpleCallback();

            switch (intent) {
                case "EXPENSE" -> {
                    double amount = data.optDouble("amount", 0.0);
                    String item = data.optString("item", data.optString("description", "Miscellaneous"));
                    CategoryMapper.MapResult mapping = CategoryMapper.mapDetailed(context, item);
                    
                    Transaction expense = new Transaction();
                    expense.setAmount(amount);
                    expense.setCategory(mapping.category);
                    expense.setSubCategory(mapping.subCategory);
                    expense.setTitle(item);
                    expense.setNote(String.format("Outflow log: %s", item));
                    transactionRepository.createItem(TransactionType.EXPENSE, expense, callback);
                }
                case "INCOME" -> {
                    double incAmount = data.optDouble("amount", 0.0);
                    String source = data.optString("item", data.optString("source", "Other"));
                    Transaction income = new Transaction();
                    income.setAmount(incAmount);
                    income.setCategory("Income");
                    income.setTitle(source);
                    income.setNote(String.format("Inflow log: %s", source));
                    transactionRepository.createItem(TransactionType.INCOME, income, callback);
                }
                case "GOAL" -> {
                    String goalTitle = data.optString("title", data.optString("item", "New Goal"));
                    double goalAmount = data.optDouble("amount", data.optDouble("targetAmount", 0.0));
                    String targetDate = data.optString("targetDate", "");
                    Transaction goal = new Transaction();
                    goal.setTitle(goalTitle);
                    goal.setTargetAmount(goalAmount);
                    goal.setTargetDate(targetDate);
                    transactionRepository.createItem(TransactionType.GOAL, goal, callback);
                }
                case "DEBT" -> {
                    String personName = data.optString("personName", data.optString("item", "Unknown"));
                    double debtAmount = data.optDouble("amount", 0.0);
                    String debtType = data.optString("type", "BORROWED");
                    String debtCategory = data.optString("category", "General");
                    String debtTargetDate = data.optString("targetDate", "");
                    Transaction debt = new Transaction();
                    debt.setTitle(personName);
                    debt.setAmount(debtAmount);
                    debt.setCategory(debtCategory);
                    debt.setTargetDate(debtTargetDate);
                    debt.setStatus(debtType);
                    transactionRepository.createItem(TransactionType.DEBT, debt, callback);
                }
                case "ALLOCATION" -> {
                    String catName = data.optString("category", "Others");
                    double allocAmount = data.optDouble("amount", 0.0);
                    transactionRepository.updateOrAddCategory(catName, allocAmount, null, false);
                    if (callback != null) callback.onSuccess(null);
                }
                case "NOTIF_READ_ALL" -> {
                    com.upreyvan.carti.repository.NotificationRepository.getInstance(context).markAllAsRead();
                    if (callback != null) callback.onSuccess(null);
                }
                default -> Log.w("AiActionHandler", "Unknown AI intent/action: " + intent);
            }
        } catch (Exception e) {
            Log.e("AiActionHandler", "Error parsing AI action JSON", e);
        }
    }

    private String normalizeToStandardType(String intent) {
        if (intent == null) return "EXPENSE";
        String s = intent.toUpperCase();
        if (s.contains("NOTIF") && s.contains("READ")) return "NOTIF_READ_ALL";
        if (s.contains("INCOME") || s.contains("KITA") || s.contains("SAHOD")) return "INCOME";
        if (s.contains("DEBT") || s.contains("UTANG")) return "DEBT";
        if (s.contains("GOAL") || s.contains("IPON")) return "GOAL";
        if (s.contains("ALLOC") || s.contains("BUDGET")) return "ALLOCATION";
        return "EXPENSE";
    }

    private class SimpleCallback implements AppwriteCallback<Map<String, Object>> {
        public SimpleCallback() { }
        @Override
        public void onSuccess(Map<String, Object> result) {
            transactionRepository.refreshTransactions();
        }
        @Override
        public void onError(Throwable error) {
            Log.e("AiActionHandler", "AI Action failed: " + (error != null ? error.getMessage() : "Unknown"));
        }
    }
}
