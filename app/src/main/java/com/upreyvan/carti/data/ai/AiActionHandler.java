package com.upreyvan.carti.data.ai;

import android.content.Context;
import android.util.Log;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.CategoryMapper;
import com.upreyvan.carti.util.Utils;
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
                    expense.setType("EXPENSE"); 
                    expense.setCategory(mapping.category);
                    expense.setSubCategory(mapping.subCategory);
                    expense.setTitle(item);
                    expense.setNote(String.format("Outflow log: %s", item));
                    transactionRepository.addTransaction(expense, callback);
                }
                case "INCOME" -> {
                    double incAmount = data.optDouble("amount", 0.0);
                    String source = data.optString("item", data.optString("source", "Other"));
                    Transaction income = new Transaction();
                    income.setAmount(incAmount);
                    income.setType("INCOME");
                    income.setCategory("Income");
                    income.setTitle(source);
                    income.setNote(String.format("Inflow log: %s", source));
                    transactionRepository.addTransaction(income, callback);
                }
                case "GOAL" -> {
                    String goalTitle = data.optString("title", data.optString("item", "New Goal"));
                    double goalAmount = data.optDouble("amount", data.optDouble("targetAmount", 0.0));
                    String targetDate = data.optString("targetDate", "");
                    Transaction goal = new Transaction();
                    goal.setType("GOAL");
                    goal.setTitle(goalTitle);
                    goal.setTargetAmount(goalAmount);
                    goal.setTargetDate(targetDate);
                    transactionRepository.addTransaction(goal, callback);
                }
                case "DEBT" -> {
                    String personName = data.optString("personName", data.optString("item", "Unknown"));
                    double debtAmount = data.optDouble("amount", 0.0);
                    String debtType = data.optString("type", "BORROWED");
                    String debtCategory = data.optString("category", "General");
                    String debtTargetDate = data.optString("targetDate", "");
                    Transaction debt = new Transaction();
                    debt.setType("DEBT");
                    debt.setTitle(personName);
                    debt.setAmount(debtAmount);
                    debt.setCategory(debtCategory);
                    debt.setTargetDate(debtTargetDate);
                    debt.setStatus(debtType);
                    transactionRepository.addTransaction(debt, callback);
                }
                case "ALLOCATION" -> {
                    String catName = data.optString("category", "Others");
                    double allocAmount = data.optDouble("amount", 0.0);
                    transactionRepository.updateOrAddCategory(catName, allocAmount, null, false);
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
