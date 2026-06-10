package com.upreyvan.carti.managers.ai;

import android.content.Context;
import android.util.Log;
import com.upreyvan.carti.utils.CategoryMapper;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalIntentParser {

    private static final String MATH_REGEX = "(?i)(?:₱|p|php|pesos?|isang|isalang)?\\s*(\\d+(?:\\.\\d+)?(?:\\s*[+\\-]\\s*\\d+(?:\\.\\d+)?)*)";
    private static final String[] FILLERS = {"ng", "mga", "para", "sa", "ko", "mo", "namin", "natin", "si", "ni", "kay", "the", "a", "an", "for", "of", "is", "on", "at", "na", "muna", "naman", "po", "opo", "eh", "din", "rin", "daw", "raw", "lng", "lang", "via", "gamit", "thru", "ako"};
    
    private static final String[] KW_GREETING = {"hi", "hello", "kamusta", "kumusta", "yo", "hoy", "yohhhh", "hey", "wats up"};
    private static final String[] KW_EXPENSE_ACTION = {"gastos", "spent", "spend", "bili", "bumili", "bayad", "nagbayad", "pambili", "check out", "order", "binili", "cost", "payment", "paid", "bought", "buy"};
    private static final String[] KW_INCOME_ACTION = {"sweldo", "salary", "allowance", "kita", "tanggap", "income", "sahod", "bonus", "raket", "benta", "pumasok", "receive", "received"};
    private static final String[] KW_ALLOCATION = {"set aside", "ipon sa", "allocate", "budget for", "tabi ko", "budget assign", "split"};
    private static final String[] KW_GOAL = {"ipon", "savings", "save", "goal", "pangarap", "target", "pondo"};
    private static final String[] KW_DEBT = {"utang", "loan", "hiram", "credit", "pautang"};
    private static final String[] KW_ASK_AMOUNT = {"magkano", "how much", "ilan", "total balance", "pera ko"};
    private static final String[] KW_ASK_CATEGORY = {"saan napunta", "anong pinaka", "anong category", "top expense"};
    private static final String[] KW_ASK_TYPE = {"anong type", "expense ba", "income ba", "classification"};
    private static final String[] KW_SUMMARY = {"summary", "report", "stats", "status", "analytics", "breakdown", "detalye", "history"};
    private static final String[] KW_ADVICE = {"tips", "advice", "paano", "bakit", "help", "coach", "tipid", "analysis"};
    private static final String[] KW_PROFANITY = {"gago", "tarantado", "puta", "tangina", "bakla", "bading", "tomboy", "t-bird", "tanga", "bobu", "bobo", "stupid", "idiot", "pangit", "panget", "sino ka", "ano ka", "lalaki ka", "babae ka", "lesbian", "gay"};

    public static JSONObject parse(Context context, String rawInput) {
        try {
            String input = rawInput.toLowerCase().trim();

            if (containsAny(input, KW_PROFANITY)) {
                return buildResult(IntentType.PROFANITY, "BLOCK", false, 1.0, "", "", "non_financial");
            }

            if (containsAny(input, KW_GREETING) && input.length() < 10) {
                return buildResult(IntentType.GREETING, "INSIGHT", false, 1.0, "hi", "", "greeting");
            }

            if (containsAny(input, KW_ADVICE)) return null;

            Pattern p = Pattern.compile(MATH_REGEX);
            Matcher m = p.matcher(input);
            double amountValue = -1;
            String amountStr = "";
            if (m.find()) {
                amountStr = m.group(0);
                amountValue = evaluateMath(m.group(1));
            }

            String cleanedItem = cleanItem(input, amountStr);
            boolean hasAmount = amountValue > 0;
            boolean hasItem = !cleanedItem.isEmpty() && cleanedItem.length() > 2;

            if (hasAmount && hasItem) {
                String localCat = CategoryMapper.map(context, cleanedItem);

                if (localCat == null && !containsAny(input, KW_INCOME_ACTION) && !containsAny(input, KW_GOAL) && !containsAny(input, KW_DEBT) && !containsAny(input, KW_ALLOCATION)) {
                    return buildResult(IntentType.UNKNOWN, "UNKNOWN", false, 0.0, cleanedItem, null, "unknown", amountValue);
                }

                if (containsAny(input, KW_INCOME_ACTION)) return buildResult(IntentType.INCOME_LOG, "LOG", true, 0.9, cleanedItem, "Income", "income", amountValue);
                if (containsAny(input, KW_ALLOCATION)) return buildResult(IntentType.ALLOCATION_LOG, "LOG", true, 0.9, cleanedItem, "Income", "allocation", amountValue);
                if (containsAny(input, KW_GOAL)) return buildResult(IntentType.GOAL_LOG, "LOG", false, 0.9, cleanedItem, "Goal", "goal", amountValue);
                if (containsAny(input, KW_DEBT)) return buildResult(IntentType.DEBT_LOG, "LOG", false, 0.9, cleanedItem, "Debt", "debt", amountValue);
                return buildResult(IntentType.EXPENSE_LOG, "LOG", true, 0.8, cleanedItem, localCat != null ? localCat : "Others", "expense", amountValue);
            }

            if (hasAmount && !hasItem) {
                return buildPending("NEED_ITEM", amountValue, "", "What is the " + amountStr + " for? 🐧");
            }


            if (!hasAmount && hasItem && cleanedItem.length() >= 3) {
                return buildPending("NEED_AMOUNT", 0, cleanedItem, "How much for " + cleanedItem + "? 🐧");
            }

            if (containsAny(input, KW_SUMMARY)) return buildResult(IntentType.SUMMARY, "INSIGHT", false, 0.9, "", "", "summary");
            if (containsAny(input, KW_ASK_AMOUNT)) return buildResult(IntentType.ASK_AMOUNT, "INSIGHT", false, 0.9, "", "", "ask_amount");
            if (containsAny(input, KW_ASK_CATEGORY)) return buildResult(IntentType.ASK_CATEGORY, "INSIGHT", false, 0.9, "", "", "ask_category");
            if (containsAny(input, KW_ASK_TYPE)) return buildResult(IntentType.ASK_TYPE, "INSIGHT", false, 0.9, "", "", "ask_type");
            if (containsAny(input, KW_PROFANITY)) return buildResult(IntentType.PROFANITY, "INSIGHT", false, 1.0, "", "", "profanity");

            return buildResult(IntentType.UNKNOWN, "UNKNOWN", false, 0.0, "", "", "");

        } catch (Exception e) {
            Log.e("LocalIntentParser", "Parse Error: " + e.getMessage());
        }
        return null;
    }

    private static JSONObject buildResult(IntentType type, String status, boolean isTransaction, double confidence, String item, String category, String intent, double amount) throws Exception {
        JSONObject result = new JSONObject();
        result.put("type", type.name());
        result.put("status", status);
        result.put("isTransaction", isTransaction);
        result.put("confidence", confidence);
        result.put("amount", amount);

        JSONObject extracted = new JSONObject();
        extracted.put("item", item);
        extracted.put("category", category);
        extracted.put("intent", intent);
        result.put("extracted", extracted);
        
        return result;
    }

    private static JSONObject buildResult(IntentType type, String status, boolean isTransaction, double confidence, String item, String category, String intent) throws Exception {
        return buildResult(type, status, isTransaction, confidence, item, category, intent, 0);
    }

    private static JSONObject buildPending(String state, double amt, String item, String msg) throws Exception {
        JSONObject res = new JSONObject();
        res.put("type", "PENDING");
        res.put("status", "PENDING");
        res.put("state", state);
        res.put("amount", amt);
        res.put("item", item);
        res.put("message", msg);
        return res;
    }

    private static String cleanItem(String input, String amountStr) {
        String cleaned = input.replace(amountStr, "").trim();
        StringBuilder regexBuilder = new StringBuilder("(?i)\\b(");
        for (String f : FILLERS) regexBuilder.append(Pattern.quote(f)).append("|");
        for (String e : KW_EXPENSE_ACTION) regexBuilder.append(Pattern.quote(e)).append("|");
        for (String i : KW_INCOME_ACTION) regexBuilder.append(Pattern.quote(i)).append("|");
        if (regexBuilder.length() > 6) regexBuilder.setLength(regexBuilder.length() - 1);
        regexBuilder.append(")\\b");

        return cleaned.replaceAll(regexBuilder.toString(), " ").replaceAll("\\s+", " ").trim();
    }

    public enum IntentType {
        GREETING, EXPENSE_LOG, INCOME_LOG, DEBT_LOG, GOAL_LOG, ALLOCATION_LOG, SUMMARY, ASK_AMOUNT, ASK_CATEGORY, ASK_TYPE, PROFANITY, UNKNOWN
    }

    private static boolean containsAny(String input, String[] keywords) {
        for (String kw : keywords) {
            if (Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE).matcher(input).find()) return true;
        }
        return false;
    }

    private static double evaluateMath(String expression) {
        try {
            String cleanExpr = expression.replaceAll("\\s+", "");
            String[] terms = cleanExpr.split("(?=[+\\-])");
            double total = 0;
            for (String term : terms) {
                if (term.isEmpty()) continue;
                String val = term.replace("+", "");
                if (val.startsWith("-")) total -= Double.parseDouble(val.substring(1));
                else total += Double.parseDouble(val);
            }
            return total;
        } catch (Exception e) { return 0; }
    }

    public static boolean isContextReset(String message) {
        if (message == null) return false;
        String msg = message.toLowerCase();
        return msg.contains("✅") || msg.contains("recorded") || msg.contains("done") || msg.contains("help you today");
    }
}
