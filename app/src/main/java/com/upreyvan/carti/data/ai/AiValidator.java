package com.upreyvan.carti.data.ai;

public class AiValidator {

    public static AiResult validate(String input) {
        String clean        = AiParser.normalize(input);
        String amount       = AiParser.extractAmount(input);  // ✅ RAW
        boolean hasAmount   = !amount.isEmpty();
        boolean isExpense   = AiParser.isExpense(clean);
        boolean isIncome    = AiParser.isIncome(clean);
        String category     = AiParser.detectCategory(clean);
        boolean hasCategory = !category.equals("Other") && !category.isEmpty();

        if (hasAmount && !isExpense && !isIncome) {
            return new AiResult("I found ₱" + amount + ". Is this EXPENSE or INCOME?", IntentType.ASK_TYPE);
        }

        if (isExpense || hasCategory) {
            if (!hasAmount) return new AiResult("Magkano ang gastos mo?", IntentType.ASK_AMOUNT);
            if (!hasCategory) return new AiResult("Anong category ng ₱" + amount + "?", IntentType.ASK_CATEGORY);
            return new AiResult("Expense logged: ₱" + amount + " for " + category, IntentType.EXPENSE_LOG);
        }

        if (isIncome) {
            if (!hasAmount) return new AiResult("Magkano ang natanggap mo?", IntentType.ASK_AMOUNT);
            return new AiResult("Income logged: ₱" + amount, IntentType.INCOME_LOG);
        }

        return new AiResult("Hindi ko naintindihan. Subukan: 'Gastos 100 pagkain'", IntentType.UNKNOWN);
    }
}