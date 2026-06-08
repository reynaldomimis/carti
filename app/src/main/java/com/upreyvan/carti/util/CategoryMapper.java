package com.upreyvan.carti.util;

import android.content.Context;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.util.Constants.Categories;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryMapper {

    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        // Food synonyms
        addSynonyms(Categories.FOOD, "food", "foood", "ffood", "foodd", "kain", "pagkain", "lunch", "dinner", "breakfast", "snack", "jollibee", "mcdo", "kfc", "chowking", "inasal", "meals");
        
        // Transportation
        addSynonyms(Categories.TRANSPORTATION, "transportation", "tran", "fare", "grab", "jeep", "tricycle", "gas", "fuel", "angkas", "joyride", "pasahe", "mrt", "lrt", "bus");
        
        // Bills
        addSynonyms(Categories.BILLS, "bills", "bill", "bayad", "utility", "meralco", "maynilad", "internet", "wifi", "kuryente", "tubig", "pldt", "converge");
        
        // Shopping
        addSynonyms(Categories.SHOPPING, "shopping", "shop", "buy", "mall", "shopee", "lazada", "grocery", "groceries", "damit", "sapatos", "bag");
        
        // Health
        addSynonyms(Categories.HEALTH, "health", "med", "doctor", "hospital", "gamot", "pharmacy", "clinic", "vitamins", "checkup");
        
        // Education
        addSynonyms(Categories.EDUCATION, "education", "school", "educ", "tuition", "books", "baon", "uniform", "pencil", "papel");
        
        // Electricity
        addSynonyms(Categories.ELECTRICITY, "electricity", "kuryente", "light", "power", "electric");
        
        // Water
        addSynonyms(Categories.WATER, "water", "tubig", "nawasa");
    }

    private static void addSynonyms(String category, String... synonyms) {
        for (String s : synonyms) {
            SYNONYMS.put(s.toLowerCase(), category);
        }
    }

    public static class MapResult {
        public String category;
        public String subCategory;
        public MapResult(String category, String subCategory) {
            this.category = category;
            this.subCategory = subCategory;
        }
    }

    public static MapResult mapDetailed(Context context, String input) {
        if (input == null || input.trim().isEmpty()) return new MapResult(Categories.OTHERS, null);
        
        String clean = input.trim().toLowerCase();

        // 1. Check Actual User Categories (Set Up Categories)
        List<com.upreyvan.carti.model.Category> actualCats = com.upreyvan.carti.data.local.CategoryManager.getInstance(context).getCategories();
        
        // Priority 1: Exact match with Sub-category or Category name
        for (com.upreyvan.carti.model.Category cat : actualCats) {
            if (cat.getName().equalsIgnoreCase(clean)) {
                if (cat.getParentCategory() != null && !cat.getParentCategory().isEmpty()) {
                    return new MapResult(cat.getParentCategory(), cat.getName());
                } else {
                    return new MapResult(cat.getName(), null);
                }
            }
        }

        // Priority 2: Contains match
        for (com.upreyvan.carti.model.Category cat : actualCats) {
            String catName = cat.getName().toLowerCase();
            if (clean.contains(catName) || catName.contains(clean)) {
                if (cat.getParentCategory() != null && !cat.getParentCategory().isEmpty()) {
                    return new MapResult(cat.getParentCategory(), cat.getName());
                } else {
                    return new MapResult(cat.getName(), null);
                }
            }
        }
        
        // 2. Check User PLAN Categories (Allocations)
        List<BudgetCategoryItem> plan = TransactionRepository.getInstance(context).getBudgetPlan();
        for (BudgetCategoryItem item : plan) {
            String catName = item.getCategoryName().toLowerCase();
            if (catName.equals(clean) || clean.contains(catName)) {
                return new MapResult(item.getCategoryName(), null);
            }
        }
        
        // 2. Direct match with defaults
        for (String category : getAllCategories()) {
            if (category.toLowerCase().equals(clean)) return new MapResult(category, null);
        }

        // 3. Synonym match
        if (SYNONYMS.containsKey(clean)) return new MapResult(SYNONYMS.get(clean), null);

        // 4. Fuzzy / Contains match
        for (Map.Entry<String, String> entry : SYNONYMS.entrySet()) {
            if (clean.contains(entry.getKey()) || isFuzzyMatch(clean, entry.getKey())) {
                return new MapResult(entry.getValue(), null);
            }
        }

        return new MapResult(Categories.OTHERS, null);
    }

    public static String map(Context context, String input) {
        return mapDetailed(context, input).category;
    }

    private static boolean isFuzzyMatch(String s1, String s2) {
        if (Math.abs(s1.length() - s2.length()) > 2) return false;
        int distance = levenshteinDistance(s1, s2);
        return distance <= 1;
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private static String[] getAllCategories() {
        return new String[]{
            Categories.FOOD, Categories.TRANSPORTATION, Categories.BILLS, 
            Categories.SHOPPING, Categories.HEALTH, Categories.EDUCATION, 
            Categories.OTHERS, Categories.ELECTRICITY, Categories.WATER, 
            Categories.RENT, Categories.INTERNET
        };
    }
}
