package com.upreyvan.carti.data.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiParser {

    // =========================
    // EXPENSE KEYWORDS (FULL LIST)
    // =========================
    public static final String[] EXPENSE_KEYWORDS = {
            // English
            "food", "expense", "spent", "spend", "buy", "bought", "purchase", "paid", "pay",
            "payment", "bill", "bills", "order", "ordered", "shopping", "checkout",
            "cashout", "withdraw", "deduct", "cost", "charge", "used money",

            // Tagalog
            "gastos", "gumastos", "nagastos", "binili", "bili", "bayad", "binayad",
            "nagbayad", "pamasahe", "kain", "pagkain", "pambili", "kuha", "kinuha",
            "utang", "hulog", "naubos", "labas pera", "bawas pera",

            // Bisaya / Cebuano
            "gasto", "gigasto", "palit", "nipalit", "bayad", "gibayran", "plite",
            "kaon", "pagkaon", "gikuha", "naubos", "giorder", "halin expense",

            // Jejemon / Slang
            "g4stos", "g4st0s", "gast0", "gastuu", "sp3nt", "spnt", "b1li", "b1l1",
            "b4yad", "p4y", "c4shout", "budol", "sh0pee", "l4zada", "minus pera", "gastooo"
    };

    // =========================
    // INCOME KEYWORDS (FULL LIST)
    // =========================
    public static final String[] INCOME_KEYWORDS = {
            "income", "salary", "earned", "earn", "received", "receive", "bonus",
            "profit", "cashin", "deposit", "credited", "allowance", "commission",
            "sale", "sold", "sweldo", "sahod", "kinita", "kita", "natanggap",
            "baon", "padala", "remittance", "benta", "nabenta", "pasok pera",
            "dumating pera", "dawat", "nadawat", "halin income", "sulod kwarta"
    };

    // =========================
    // SUPER NORMALIZER
    // =========================
    public static String normalize(String input) {
        if (input == null) return "";
        String normalized = input.toLowerCase()
                .replace("0", "o").replace("1", "i")
                .replace("3", "e").replace("4", "a")
                .replace("5", "s").replace("7", "t")
                .replace("@", "a").replace("$", "s");
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");
        normalized = normalized.replaceAll("(.)\\1{2,}", "$1");
        return normalized.trim();
    }

    // =========================
    // SMART AMOUNT EXTRACTION
    // =========================
    public static String extractAmount(String input) {
        if (input == null) return "";
        Pattern p = Pattern.compile("\\d+(\\.\\d{1,2})?");
        Matcher m = p.matcher(input);
        String largestAmount = "";
        double maxVal = -1;
        while (m.find()) {
            try {
                double currentVal = Double.parseDouble(m.group());
                if (currentVal > maxVal) {
                    maxVal = currentVal;
                    largestAmount = m.group();
                }
            } catch (Exception e) {}
        }
        return largestAmount;
    }

    // =========================
    // TYPE DETECTION
    // =========================
    public static boolean isIncome(String input) {
        String text = normalize(input);
        for (String k : INCOME_KEYWORDS) {
            if (text.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    public static boolean isExpense(String input) {
        String text = normalize(input);
        for (String k : EXPENSE_KEYWORDS) {
            if (text.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    // =========================
    // DETECT CATEGORY
    // =========================
    public static String detectCategory(String input) {
        String text = normalize(input);

        if (text.matches(".*(meralco|kuryente|electric|mclight|ilaw).*")) return "Electricity";
        if (text.matches(".*(water|tubig|nawasa|maynilad|manilawater).*")) return "Water";
        if (text.matches(".*(wifi|internet|pldt|converge|fiber|sky).*")) return "Internet";
        if (text.matches(".*(food|kain|pagkain|mcdo|jollibee|kfc|starbucks|eat|snack|gutom).*")) return "Food";
        if (text.matches(".*(fare|pamasahe|jeep|tricycle|grab|angkas|joyride|taxi|bus|lrt|mrt|moveit).*")) return "Fare";
        if (text.matches(".*(load|promo|data|gosurf|surfaya|tm|globe|smart|tnt|dito|paload).*")) return "Load";
        if (text.matches(".*(debt|utang|bayad|hiram|loan|lending|cc|credit card).*")) return "Debt";
        if (text.matches(".*(store|tindahan|sari-sari|bili|tingi|groceries|puregold).*")) return "Sari-sari Store";

        return "Other";
    }

    // =========================
    // IDENTITY DETECTION
    // =========================
    public static boolean isIdentityInquiry(String input) {
        String text = normalize(input);
        return text.matches(".*(sino ka|who are you|ano pangalan mo|what is your name|anong pangalan mo|who created you|sino gumawa|sinong gumawa|gumawa sayo|creator|developer|denevelop|upreyvan|carti ai|maker|made you|sino ka ba|kilala mo si upreyvan|anong app ito|ano ka|what are you|saan ka galing|taga saan ka|purpose mo|identity|sinong creator).*");
    }

    // =========================
    // OFFLINE CAPABILITY DETECTION
    // =========================
    public static boolean isOfflineInquiry(String input) {
        String text = normalize(input);
        return text.matches(".*(offline|internet|wifi|data|kailangan ba ng net|no connection|connection|gumagana ba pag walang|without internet|working offline|no wifi|walang net|may internet).*");
    }

    // =========================
    // COMPLIMENT DETECTION
    // =========================
    public static boolean isCompliment(String input) {
        String text = normalize(input);
        return text.matches(".*(galing|ayus|nice|lodi|wow|good job|pogi|magaling|solid|astig|petmalu|mahusay|ang lupet|the best|idol|lodi cakes|galing ah|ayos ah|nice one).*");
    }

    // =========================
    // LAUGHTER / JOKE DETECTION
    // =========================
    public static boolean isLaughter(String input) {
        String text = normalize(input);
        return text.matches(".*(haha|hehe|hihi|hoho|lmao|lol|rofl|joke|biro|tawa|nakakatawa|funny).*");
    }

    // =========================
    // TRAINING ACTIVATION
    // =========================
    public static boolean isTrainingActivation(String input) {
        String text = normalize(input);
        return text.matches(".*(turuan kita|tandaan mo|learn mode|training mode|mag-aral tayo|ituro ko sayo).*");
    }

    /**
     * Extracts a keyword and its category from a training message.
     * Example: "Ang Bigas ay para sa Food" -> returns ["bigas", "Food"]
     */
    public static String[] extractTrainingData(String input) {
        String text = input.toLowerCase();
        
        // Match common patterns: "X is Y", "X for Y", "X ay para sa Y", "X category Y"
        Pattern p = Pattern.compile("(?i)(?:ang\\s+)?(\\w+)\\s+(?:is|para sa|ay|category|sa)\\s+(\\w+)");
        Matcher m = p.matcher(text);
        
        if (m.find()) {
            String keyword = m.group(1).trim();
            String category = m.group(2).trim();
            
            // Capitalize first letter of category to match existing categories
            if (!category.isEmpty()) {
                category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
            }
            
            return new String[]{keyword, category};
        }
        
        return null;
    }
}