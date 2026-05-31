package com.upreyvan.carti.data.ai;

import android.content.Context;
import android.util.Log;

import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Utils;
import org.json.JSONObject;
import java.util.Map;

public class AiActionHandler {
    private final ApiHelper apiHelper;
    private final TransactionRepository transactionRepository;
    private final Context context;

    public AiActionHandler(Context context) {
        this.context = context;
        this.apiHelper = new ApiHelper(context);
        this.transactionRepository = TransactionRepository.getInstance(context);
    }

    public void executeAction(JSONObject json) {
        executeAction(json, null);
    }

    public void executeAction(JSONObject json, AppwriteManager.AppwriteCallback<Map<String, Object>> customCallback) {
        try {
            if (json == null) return;
            String rawIntent = json.optString("intent", json.optString("action", "EXPENSE"));
            String intent = normalizeToStandardType(rawIntent);
            
            JSONObject data = json.has("data") ? json.optJSONObject("data") : json;
            if (data == null) data = json;

            Log.d("AiActionHandler", "Executing Normalized AI Action: " + intent);

            AppwriteManager.AppwriteCallback<Map<String, Object>> callback = 
                (customCallback != null) ? customCallback : new SimpleCallback(intent + " processed");

            switch (intent) {
                case "EXPENSE": {
                    double amount = data.optDouble("amount", 0.0);
                    String item = data.optString("item", data.optString("description", "Miscellaneous"));
                    String category = CategoryMapper.getCategory(item, data.optString("category", "Others"));

                    Transaction expense = new Transaction();
                    expense.setAmount(amount);
                    expense.setType("EXPENSE"); 
                    expense.setCategory(category);
                    expense.setTitle(item);
                    expense.setDescription(String.format("Outflow log: %s", item));
                    
                    transactionRepository.addTransaction(expense, callback);
                    break;
                }

                case "INCOME": {
                    double incAmount = data.optDouble("amount", 0.0);
                    String source = data.optString("item", data.optString("source", "Other"));
                    
                    Transaction income = new Transaction();
                    income.setAmount(incAmount);
                    income.setType("INCOME");
                    income.setCategory("Income");
                    income.setTitle(source);
                    income.setDescription(String.format("Inflow log: %s", source));
                    
                    transactionRepository.addTransaction(income, callback);
                    break;
                }

                case "GOAL": {
                    String goalTitle = data.optString("title", data.optString("item", "New Goal"));
                    double goalAmount = data.optDouble("amount", data.optDouble("targetAmount", 0.0));
                    String targetDate = data.optString("targetDate", "");

                    apiHelper.addGoal(goalTitle, goalAmount, targetDate, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                        @Override public void onSuccess(Map<String, Object> result) { transactionRepository.refreshTransactions(); callback.onSuccess(result); }
                        @Override public void onError(Throwable error) { callback.onError(error); }
                    });
                    break;
                }

                case "DEBT": {
                    String personName = data.optString("personName", data.optString("item", "Unknown"));
                    double debtAmount = data.optDouble("amount", 0.0);
                    String debtType = data.optString("type", "BORROWED");
                    String debtCategory = data.optString("category", "General");
                    String debtTargetDate = data.optString("targetDate", "");

                    apiHelper.addDebt(personName, debtAmount, debtType, debtCategory, debtTargetDate, "NO_REMINDER", "", new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                        @Override public void onSuccess(Map<String, Object> result) { transactionRepository.refreshTransactions(); callback.onSuccess(result); }
                        @Override public void onError(Throwable error) { callback.onError(error); }
                    });
                    break;
                }

                case "ALLOCATION": {
                    String catName = data.optString("category", "Others");
                    double allocAmount = data.optDouble("amount", 0.0);
                    BudgetManager.getInstance(context).updateOrAddCategory(catName, allocAmount, null);
                    
                    Transaction alloc = new Transaction();
                    alloc.setAmount(allocAmount);
                    alloc.setType("ALLOCATION"); 
                    alloc.setCategory(catName);
                    alloc.setTitle(catName);
                    alloc.setAllocatedTo(catName);
                    alloc.setAllocationMonth(Utils.getCurrentTimestamp());
                    
                    transactionRepository.addTransaction(alloc, callback);
                    break;
                }

                default:
                    Log.w("AiActionHandler", "Unknown AI intent/action: " + intent);
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

    private class SimpleCallback implements AppwriteManager.AppwriteCallback<Map<String, Object>> {
        private final String message;
        public SimpleCallback(String message) { this.message = message; }

        @Override
        public void onSuccess(Map<String, Object> result) {
            Log.d("AiActionHandler", "Success: " + message);
            transactionRepository.refreshTransactions();
        }

        @Override
        public void onError(Throwable error) {
            Log.e("AiActionHandler", "AI Action failed: " + (error != null ? error.getMessage() : "Unknown"));
        }
    }
}
