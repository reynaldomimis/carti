package com.upreyvan.carti.data.ai;

import android.content.Context;
import android.util.Log;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.util.ToastHelper;
import org.json.JSONObject;
import java.util.Map;

/**
 * Senior Level: Orchestrator that executes database actions determined by the AI.
 */
public class AiActionHandler {
    private final ApiHelper apiHelper;
    private final Context context;

    public AiActionHandler(Context context) {
        this.context = context;
        this.apiHelper = new ApiHelper(context);
    }

    public void executeAction(JSONObject actionJson) {
        try {
            String actionType = actionJson.optString("action");
            JSONObject data = actionJson.optJSONObject("data");
            if (data == null) return;

            Log.d("AiActionHandler", "Executing AI Action: " + actionType);

            switch (actionType) {
                case "ADD_EXPENSE":
                    apiHelper.addTransaction(
                        data.getDouble("amount"),
                        "EXPENSE",
                        data.getString("category"),
                        data.optString("note", ""),
                        new SimpleCallback("Expense added")
                    );
                    break;

                case "ADD_INCOME":
                    apiHelper.addIncome(
                        data.getString("source"),
                        data.getDouble("amount"),
                        new SimpleCallback("Income added")
                    );
                    break;

                case "ADD_GOAL":
                    apiHelper.addGoal(
                        data.getString("title"),
                        data.getDouble("targetAmount"),
                        data.optString("targetDate", ""),
                        new SimpleCallback("Goal set")
                    );
                    break;

                case "ADD_DEBT":
                    apiHelper.addDebt(
                        data.getString("personName"),
                        data.getDouble("amount"),
                        data.optString("type", "BORROWED"),
                        data.optString("category", "General"),
                        data.optString("targetDate", ""),
                        "NO_REMINDER",
                        data.optString("notes", ""),
                        new SimpleCallback("Debt logged")
                    );
                    break;

                default:
                    Log.w("AiActionHandler", "Unknown AI action: " + actionType);
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
            Log.d("AiActionHandler", message);
            ToastHelper.show(context, message, ToastHelper.Status.SUCCESS);
        }

        @Override
        public void onError(Throwable error) {
            Log.e("AiActionHandler", "AI Action failed: " + (error != null ? error.getMessage() : "Unknown"));
        }
    }
}
