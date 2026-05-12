package com.upreyvan.carti.data.ai;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class CartiAiManager {

    private static CartiAiManager instance;
    private final Context context;

    // Full conversation history
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

        // Identity Check
        if (AiParser.isIdentityInquiry(input)) {
            return new AiResult(
                    "Ako si Carti AI, ang iyong personal finance assistant na binuo at denevelop ng Team Upreyvan. " +
                            "Nandito ako para tulungan ang iyong pamilya sa pag-manage ng budget at expenses!",
                    IntentType.UNKNOWN
            );
        }

        // Offline Check
        if (AiParser.isOfflineInquiry(input)) {
            return new AiResult(
                    "Oo naman! 100% offline ang Carti. Maaari mong i-log ang iyong expenses at gamitin ang AI kahit walang internet o data connection. " +
                            "Your data is safe and stays on your device!",
                    IntentType.UNKNOWN
            );
        }

        // Compliment Check
        if (AiParser.isCompliment(input)) {
            String[] responses = {
                    "Salamat! Ginagawa ko lang ang makakaya ko para sa inyo. 😊",
                    "Naks! Salamat sa pag-puri, lalo akong gaganahan mag-track ng expenses niyo! 🚀",
                    "Wow, salamat! Basta para sa budget ng pamilya, laging handa si Carti. 💪",
                    "Idol din kita! Basta wag lang kalimutan i-log ang expenses ha? Hehe."
            };
            return new AiResult(responses[(int) (Math.random() * responses.length)], IntentType.UNKNOWN);
        }

        // Laughter / Joke Check
        if (AiParser.isLaughter(input)) {
            String[] responses = {
                    "Hahaha! Nakakatuwa naman. 😂",
                    "Hehe, buti naman at napasaya kita! 🐧",
                    "Hahaha! Seryoso tayo sa budget pero dapat happy din ang pamilya! ✨",
                    "Benta 'yun ah! Hahaha! 😂"
            };
            return new AiResult(responses[(int) (Math.random() * responses.length)], IntentType.UNKNOWN);
        }

        // Add to history
        conversationHistory.add(input);

        String resolvedAmount   = "";
        String resolvedCategory = "";
        Boolean resolvedType    = null;

        for (String msg : conversationHistory) {
            String clean    = AiParser.normalize(msg);
            String amount   = AiParser.extractAmount(msg);

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

        if (hasType && !hasAmount) {
            return new AiResult("Magkano?", IntentType.ASK_AMOUNT);
        }

        if (hasAmount && !hasType) {
            return new AiResult(
                    "₱" + resolvedAmount + " — Expense o Income?",
                    IntentType.ASK_TYPE
            );
        }

        return new AiResult(
                "Hindi ko naintindihan. Subukan: 'Gastos 100 pagkain'",
                IntentType.UNKNOWN
        );
    }
}