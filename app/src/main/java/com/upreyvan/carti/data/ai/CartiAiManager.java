package com.upreyvan.carti.data.ai;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class CartiAiManager {

    private static CartiAiManager instance;
    private final Context context;

    // ✅ Full conversation history
    private final List<String> conversationHistory = new ArrayList<>();

    private CartiAiManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized CartiAiManager getInstance(Context context) {
        if (instance == null) instance = new CartiAiManager(context);
        return instance;
    }

    public void resetState() {
        conversationHistory.clear();
    }

    public AiResult processMessage(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new AiResult("Please enter a message.", IntentType.UNKNOWN);
        }

        // ✅ Add to history
        conversationHistory.add(input);

        // ✅ Scan entire history para hanapin lahat ng info
        String resolvedAmount   = "";
        String resolvedCategory = "";
        Boolean resolvedType    = null; // true = expense, false = income

        for (String msg : conversationHistory) {
            String clean    = AiParser.normalize(msg);
            String amount   = AiParser.extractAmount(msg);  // RAW

            if (!amount.isEmpty()) resolvedAmount = amount;

            String cat = AiParser.detectCategory(clean);
            if (!cat.equals("Other")) resolvedCategory = cat;

            if (AiParser.isExpense(clean)) resolvedType = true;
            if (AiParser.isIncome(clean))  resolvedType = false;
        }

        boolean hasAmount   = !resolvedAmount.isEmpty();
        boolean hasCategory = !resolvedCategory.isEmpty();
        boolean hasType     = resolvedType != null;

        // ✅ May lahat na — i-log na
        if (hasType && hasAmount) {
            if (resolvedType) {
                // EXPENSE — kailangan pa ng category
                if (hasCategory) {
                    resetState();
                    return new AiResult(
                            "✅ Na-log na! Expense: ₱" + resolvedAmount + " para sa " + resolvedCategory,
                            IntentType.EXPENSE_LOG
                    );
                } else {
                    return new AiResult(
                            "Anong category ng ₱" + resolvedAmount + "? (Food, Fare, Load, atbp.)",
                            IntentType.ASK_CATEGORY
                    );
                }
            } else {
                // INCOME — di na kailangan ng category
                resetState();
                return new AiResult(
                        "✅ Na-log na! Income: ₱" + resolvedAmount,
                        IntentType.INCOME_LOG
                );
            }
        }

        // ✅ May type pero walang amount
        if (hasType && !hasAmount) {
            return new AiResult("Magkano?", IntentType.ASK_AMOUNT);
        }

        // ✅ May amount pero walang type
        if (hasAmount && !hasType) {
            return new AiResult(
                    "₱" + resolvedAmount + " — Expense o Income?",
                    IntentType.ASK_TYPE
            );
        }

        // ✅ Walang context — hindi naintindihan
        return new AiResult(
                "Hindi ko naintindihan. Subukan: 'Gastos 100 pagkain'",
                IntentType.UNKNOWN
        );
    }
}