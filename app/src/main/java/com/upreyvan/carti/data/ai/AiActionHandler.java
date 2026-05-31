package com.upreyvan.carti.data.ai;

import android.content.Context;
import android.util.Log;

import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import org.json.JSONObject;
import java.util.Map;

public class AiActionHandler {
    private final ApiHelper apiHelper;
    private final Context context;

    public AiActionHandler(Context context) {
        this.context = context;
        this.apiHelper = new ApiHelper(context);
    }

    public void executeAction(JSONObject json) {
        executeAction(json, null);
    }

    public void executeAction(JSONObject json, AppwriteManager.AppwriteCallback<Map<String, Object>> customCallback) {
        try {
            String intent = json.optString("intent", json.optString("action"));
            if (intent.isEmpty()) {
                Log.w("AiActionHandler", "No intent or action found in JSON");
                return;
            }

            JSONObject data = json.has("data") ? json.optJSONObject("data") : json;
            
            Log.d("AiActionHandler", "Executing AI Action: " + intent);

            AppwriteManager.AppwriteCallback<Map<String, Object>> callback = 
                (customCallback != null) ? customCallback : new SimpleCallback(intent + " processed");

            switch (intent.toUpperCase()) {
                case "EXPENSE":
                case "ADD_EXPENSE":
                    double amount = data.optDouble("amount", 0.0);
                    String category = data.optString("category", "unknown");
                    apiHelper.addTransaction(
                        amount,
                        "EXPENSE",
                        category,
                        data.optString("description", data.optString("note", "")),
                        callback
                    );
                    BudgetManager.getInstance(context).addExpenseToCategory(category, amount);
                    break;

                case "INCOME":
                case "ADD_INCOME":
                    apiHelper.addIncome(
                        data.optString("description", data.optString("source", "Other")),
                        data.optDouble("amount", 0.0),
                        callback
                    );
                    break;

                case "GOAL":
                case "ADD_GOAL":
                    apiHelper.addGoal(
                        data.optString("title", data.optString("description", "New Goal")),
                        data.optDouble("amount", data.optDouble("targetAmount", 0.0)),
                        data.optString("targetDate", ""),
                        callback
                    );
                    break;

                case "DEBT":
                case "ADD_DEBT":
                    apiHelper.addDebt(
                        data.optString("title", data.optString("personName", "Unknown")),
                        data.optDouble("amount", 0.0),
                        data.optString("type", "BORROWED"),
                        data.optString("category", "General"),
                        data.optString("targetDate", ""),
                        "NO_REMINDER",
                        data.optString("description", data.optString("notes", "")),
                        callback
                    );
                    break;

                case "ALLOCATE":
                case "UPDATE_BUDGET":
                    BudgetManager.getInstance(context).updateOrAddCategory(
                        data.getString("category"),
                        data.getDouble("amount"),
                        data.optString("parent", null)
                    );
                    break;

                default:
                    Log.w("AiActionHandler", "Unknown AI intent/action: " + intent);
            }
        } catch (Exception e) {
            Log.e("AiActionHandler", "Error parsing AI action JSON", e);
        }
    }

    private class SimpleCallback implements AppwriteManager.AppwriteCallback<Map<String, Object>> {
        private final String message;
        public SimpleCallback(String message) { this.message = message; }

        @Override
        public void onSuccess(Map<String, Object> result) {
            Log.d("AiActionHandler", "Success: " + message);
        }

        @Override
        public void onError(Throwable error) {
            Log.e("AiActionHandler", "AI Action failed: " + (error != null ? error.getMessage() : "Unknown"));
        }
    }
}
