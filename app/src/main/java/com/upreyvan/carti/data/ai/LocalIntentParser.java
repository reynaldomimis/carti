package com.upreyvan.carti.data.ai;

import android.util.Log;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalIntentParser {

    private static final String MATH_REGEX = "(?i)(?:₱|p|php|pesos?|isang|isalang)?\\s*(\\d+(?:\\.\\d+)?(?:\\s*[+\\-]\\s*\\d+(?:\\.\\d+)?)*)";
    
    private static final String[] PERIOD_TODAY = {"today", "ngayon", "ngayun", "ngaun", "araw", "todai", "ngayong araw"};
    private static final String[] PERIOD_YESTERDAY = {"yesterday", "kahapon", "khapon", "khpon", "yestaday", "kagabi"};
    private static final String[] PERIOD_THIS_WEEK = {"this week", "ngayong linggo", "week", "linggong ito"};
    private static final String[] PERIOD_LAST_WEEK = {"last week", "nakaraang linggo", "nakaraang week"};
    
    // --- KEYWORD AGGREGATORS (Centralized via CategoryMapper) ---
    private static final String[] KW_EXPENSE = concat(
            CategoryMapper.CAT_FOOD,
            CategoryMapper.CAT_BILLS,
            CategoryMapper.CAT_TRANSPORT,
            CategoryMapper.CAT_GADGETS,
            CategoryMapper.CAT_APPLIANCES,
            CategoryMapper.CAT_SHOPPING,
            CategoryMapper.CAT_PERSONAL,
            CategoryMapper.CAT_HEALTH,
            CategoryMapper.CAT_EDUCATION,
            CategoryMapper.CAT_WORK,
            CategoryMapper.CAT_OTHERS
    );

    private static final String[] KW_INCOME = CategoryMapper.CAT_INCOME;

    private static final String[] KW_DEBT = {
            "utang", "loan", "loans", "debt", "debts", "hiram", "credit", "cc", "amortization", "amort", "hulog", "payable", "receivable", "pautang", "utng", "utang q", "utang ko", "utang namin", "utang natin", "credit card", "cc balance", "cc bill", "installment", "hulog q", "hulog ko", "bayad utang", "bayad utng", "nautang", "nautang na pera", "hirman", "pinahiram", "lend", "borrowed", "borrow", "lender", "bombay", "5-6", "settle debt", "loan status", "loan list", "utang status", "utang list", "utang breakdown", "balanse q", "balanse ko", "balance q", "balance ko", "loan amount", "money lender", "bank loan", "car loan", "house loan", "sss loan", "pagibig loan", "gsis loan", "calamity loan", "salary loan", "personal loan", "educational loan", "credit limit", "overdue", "penalty", "arrears", "debt collection", "debt collector", "borrow from", "borrowed from", "credit score", "credit check", "financing", "loan plan", "loan app", "tala", "billease", "digido", "home credit", "lazada loan", "spaylater", "spay later", "gcash loan", "gloan", "ggives", "cimb loan", "owe", "owing", "owes", "money owed", "settlement", "settle", "settled", "paid debt", "paid loan", "bayad na utang", "bayad na loan"
    };

    private static final String[] KW_GOAL = {
            "ipon", "naipon", "savings", "save", "goal", "goals", "pangarap", "target", "pondo", "fund", "funds", "ipon q", "ipon ko", "ipon namin", "ipon natin", "savings q", "savings ko", "savings namin", "savings natin", "goal q", "goal ko", "goal namin", "goal natin", "goal status", "ipon status", "pondo q", "itinabi", "itatabi", "nakatago", "nakatago q", "nakatago ko", "dream goal", "na-save", "nasave", "ipun", "savngs", "trget", "total ipon", "emergency fund", "travel fund", "savings goal", "retirement fund", "wedding fund", "education fund", "house fund", "car fund", "invest", "investment", "itatabi", "itinabi na", "itatabi pa", "nakatago na", "ipinon", "saving", "saving up", "save money", "ipon challenge", "ipon-list", "savings-list", "goal-list", "target-amount", "target-date", "savings-plan", "goal-plan", "ipon-plan", "ipon summary", "savings summary", "goal summary", "ipon breakdown", "savings breakdown", "goal breakdown", "savings target", "goal target", "ipon amount", "savings amount", "ipon progress", "goal progress", "savings progress", "saving progress", "naitabi", "naipon na", "nakatabi na", "itinago", "ipon ko lahat", "savings ko lahat", "total savings", "total goals", "total ipon", "saved amount", "saved total"
    };

    private static final String[] KW_ALLOC = {
            "allocation", "allocate", "budget", "binudget", "nilaan", "laan", "alokasyon", "aloct", "bdgt", "bgt", "nakatabi", "set aside", "budget plan", "hati", "partition", "setsayde", "pambudget", "pambudgetan", "pambgt", "pambudgt", "laan q", "tabi q", "budget q", "aloct q", "aloc q", "nka-laan", "nka-tabi", "nka-budget", "nka-aloct", "nka-bdgt", "budget namin", "budget nyo", "budget natin", "alc", "aloc", "alksyon", "budjet", "budgt ko", "nabudget na", "envelope", "envelope system", "distribution", "hati-hati", "partition money", "spending limit", "budget limit", "allocate amount", "budget amount", "budget setting", "budget category", "budgeter", "budget summary", "binahagi", "bahagi", "bahagi ko", "bahagi q", "pinamahagi", "naipamahagi", "apportion", "budget plan ko", "budget plan q", "allocation plan", "spending plan", "money plan", "monthly budget", "weekly budget", "daily budget", "budget tracker", "allocation summary", "allocation breakdown", "budget breakdown", "alloted", "allotment", "allot", "allotting", "alloted amount", "allocated funds", "budget funds", "budgeted", "budgeted amount", "nilaan para sa", "budget para sa", "nakalaan na", "nakatabi na", "nka-laan na", "nka-tabi na", "nka-budget na", "partitioned", "divided", "distributed", "allocated money", "budgeted money"
    };

    private static final String[] KW_SUMMARY = {
            "magkano", "magkanu", "mkano", "mkanu", "ilan", "ilang", "how much", "hm", "total", "patingin", "check", "stats", "status", "report", "summary", "lista", "listahan", "record", "history", "analytics", "graph", "chart", "breakdown", "detalye", "details", "tabular", "table", "pakita", "checking balance", "info", "information", "overall", "full status", "status q", "status ko", "analysis", "insight", "insights", "natira", "current funds", "cash left", "balns", "remaining cash", "remaining balance"
    };

    private static final String[] LOG_INCOME = {"sweldo", "salary", "allowance", "kita", "tanggap", "income", "sahod", "bonus", "raket", "benta", "pumasok", "remittance", "padala", "deposit", "commission", "tip", "sold", "wire transfer", "bank transfer", "yearend", "payout", "pamasok", "shod", "swldo", "cash-in", "pamasok", "nakuha", "natanggap", "bigay", "regalo", "pamasko", "aguinaldo", "sideline", "side hustle"};
    private static final String[] LOG_EXPENSE = {"spent", "gastos", "bayad", "bili", "bumili", "nagbayad", "nakabili", "order", "pambili", "checkout", "spent", "gasta", "ngbayad", "ngastos", "ng-order", "pambly", "binili", "payment", "paid", "bought", "buy", "gastusin", "checkout", "checkout q", "nag-order", "ng-order", "pambili", "pambly", "pambayad", "pambayd", "binili", "binli", "gastusin", "expenditures", "cost", "bill", "billing", "bayarin"};
    private static final String[] KW_ADVICE = {"bakit", "paano", "tips", "advice", "analysis", "why", "how", "help", "tip", "tulong", "advice q", "bakit mabilis", "paano makatipid"};

    private static final String DATE_PATTERN = "(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?|\\d{1,2})\\s+\\d{1,2}|\\d{1,2}/\\d{1,2}";
    private static final String DATE_RANGE_REGEX = "(?i)(" + DATE_PATTERN + ")\\s*(?:to|hanggang|until|-)\\s*(" + DATE_PATTERN + ")";

    public static JSONObject parse(String input) {
        try {
            input = input.toLowerCase().trim();

            if (containsAny(input, KW_ADVICE)) return null;

            if (input.isEmpty() || input.equals("hello") || input.equals("hi") || input.equals("hey") || input.equals("yo")) {
                JSONObject result = new JSONObject();
                result.put("intent", "LOCAL_GREETING");
                result.put("local", true);
                return result;
            }

            Pattern p = Pattern.compile(MATH_REGEX);
            Matcher m = p.matcher(input);

            double amount = -1;
            String amountStr = "";
            if (m.find()) {
                amountStr = m.group(0);
                amount = evaluateMath(m.group(1));
            }

            String cleaned = input.replace(amountStr, "").replaceAll("[^a-zA-Z\\s]", " ").trim();

            // SENIOR LOGIC: Check if the 'cleaned' text is just an action word (e.g., "Gastos", "Bili")
            String substance = cleaned.replaceAll("(?i)\\b(gastos|spent|bili|bumili|bayad|nagbayad|pambili|check out|order|income|sahod|kita|sweldo|raket|benta|gastusin|cost|payment|paid|bought|buy)\\b", "").trim();

            // ANALYTICS / SUMMARY CHECK
            if (containsAny(input, KW_SUMMARY) || (amount == -1 && containsAny(input, KW_ALLOC))) {
                return buildDynamicQuery(input);
            }

            // SLOT FILLING: Amount Only (or just action words like "Gastos 100")
            if (amount != -1 && (substance.isEmpty() || substance.length() < 2)) {
                return buildMissingInfoPrompt("What is the " + amountStr + " for? 🐧");
            }

            // SLOT FILLING: Item Only
            if (amount == -1 && !cleaned.isEmpty() && cleaned.length() > 2) {
                if (containsAny(input, concat(KW_EXPENSE, KW_INCOME, KW_DEBT, KW_GOAL, KW_ALLOC))) {
                    return buildMissingInfoPrompt("How much for '" + cleaned + "'? 🐧");
                }
            }

            // FULL LOG DETECTION
            if (amount != -1 && !cleaned.isEmpty()) {
                String intent = "ADD_EXPENSE";
                if (containsAny(input, LOG_INCOME)) intent = "ADD_INCOME";
                else if (containsAny(input, KW_DEBT)) intent = "ADD_DEBT";
                else if (containsAny(input, KW_GOAL)) intent = "ADD_GOAL";
                else if (containsAny(input, KW_ALLOC)) intent = "ALLOCATE";
                return buildLocalLog(intent, amount, cleaned);
            }

        } catch (Exception e) {
            Log.e("LocalIntentParser", "Parse Error: " + e.getMessage());
        }
        return null;
    }

    private static JSONObject buildMissingInfoPrompt(String question) throws Exception {
        JSONObject result = new JSONObject();
        result.put("intent", "LOCAL_QUESTION");
        result.put("message", question);
        result.put("local", true);
        return result;
    }

    private static JSONObject buildLocalLog(String intent, double amount, String item) throws Exception {
        StringBuilder regexBuilder = new StringBuilder("(?i)\\b(");
        String[] fillers = {"ng", "mga", "para", "sa", "ko", "mo", "namin", "natin", "si", "ni", "kay", "the", "a", "an", "for", "of", "is", "on", "at", "na", "muna", "naman", "po", "opo", "eh", "din", "rin", "daw", "raw", "lng", "lang", "via", "gamit", "thru", "ako"};
        
        for (String f : fillers) regexBuilder.append(Pattern.quote(f)).append("|");
        for (String e : LOG_EXPENSE) regexBuilder.append(Pattern.quote(e)).append("|");
        for (String i : LOG_INCOME) regexBuilder.append(Pattern.quote(i)).append("|");
        
        if (regexBuilder.length() > 6) regexBuilder.setLength(regexBuilder.length() - 1);
        regexBuilder.append(")\\b");

        String cleanItem = item.replaceAll(regexBuilder.toString(), " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        if (cleanItem.isEmpty()) {
            cleanItem = item.substring(0, 1).toUpperCase() + item.substring(1);
        } else {
            cleanItem = cleanItem.substring(0, 1).toUpperCase() + cleanItem.substring(1);
        }

        JSONObject data = new JSONObject();
        data.put("amount", amount);
        data.put("item", cleanItem);
        
        // SENIOR FIX: Use "Others" as the absolute fallback for Category, NOT the Intent name.
        data.put("category", CategoryMapper.getCategory(cleanItem, "Others"));

        JSONObject result = new JSONObject();
        result.put("intent", intent);
        result.put("data", data);
        result.put("local", true);
        return result;
    }

    private static JSONObject buildDynamicQuery(String input) throws Exception {
        JSONObject data = new JSONObject();

        boolean isGeneralStatus = containsAny(input, new String[]{"status", "summary", "report", "stats", "everything", "all", "overall", "total"});

        data.put("want_balance", isGeneralStatus || containsAny(input, new String[]{"balanse", "balance", "pera", "money", "cash", "natira"}));
        data.put("want_expense", isGeneralStatus || containsAny(input, KW_EXPENSE));
        data.put("want_income", isGeneralStatus || containsAny(input, KW_INCOME));
        data.put("want_debt", containsAny(input, KW_DEBT));
        data.put("want_goal", containsAny(input, KW_GOAL));
        data.put("want_alloc", containsAny(input, KW_ALLOC));
        data.put("is_list", containsAny(input, new String[]{"lista", "listahan", "record", "history", "details", "table"}));

        String period = "MONTH";
        Matcher dateMatcher = Pattern.compile(DATE_RANGE_REGEX).matcher(input);
        if (dateMatcher.find()) {
            period = "CUSTOM";
            data.put("startDate", dateMatcher.group(1));
            data.put("endDate", dateMatcher.group(2));
        } else if (containsAny(input, PERIOD_TODAY)) period = "TODAY";
        else if (containsAny(input, PERIOD_YESTERDAY)) period = "YESTERDAY";
        else if (containsAny(input, PERIOD_LAST_WEEK)) period = "LAST_WEEK";
        else if (containsAny(input, PERIOD_THIS_WEEK)) period = "THIS_WEEK";

        data.put("period", period);
        if (data.optBoolean("want_alloc")) data.put("targetCategory", CategoryMapper.getCategory(input, "ALL"));

        JSONObject result = new JSONObject();
        result.put("intent", "LOCAL_COMPUTE_DYNAMIC");
        result.put("data", data);
        result.put("local", true);
        return result;
    }

    public static boolean isContextReset(String message) {
        if (message == null) return false;
        String msg = message.toLowerCase();
        
        // Slot-filling questions from AI should NOT reset context
        if (msg.contains("how much for '") || msg.contains("what is the") || msg.contains("what is this")) {
            return false;
        }

        if (msg.contains("i'm carti") || msg.contains("i am carti") || msg.contains("welcome to your family budget")) {
            return false;
        }

        // Hard Success Markers
        if (msg.contains("✅") || msg.contains("recorded") || msg.contains("done") || msg.contains("recorded locally")) return true;
        
        // Greeting Markers (Only those that ask "How can I help")
        if (msg.contains("yes?") || msg.contains("help you today")) return true;

        // Analysis / Summary Keywords
        if (containsAny(msg, KW_SUMMARY)) return true;
        if (containsAny(msg, KW_ADVICE)) return true;
        
        // Advice markers (Common AI response patterns)
        if (msg.contains("it seems") || msg.contains("consider") || msg.contains("planning") || msg.contains("suggest")) return true;

        return false;
    }

    private static boolean containsAny(String input, String[] keywords) {
        for (String kw : keywords) {
            if (Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE).matcher(input).find()) return true;
        }
        return false;
    }

    private static String[] concat(String[]... arrays) {
        int length = 0;
        for (String[] array : arrays) length += array.length;
        String[] result = new String[length];
        int pos = 0;
        for (String[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
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
}