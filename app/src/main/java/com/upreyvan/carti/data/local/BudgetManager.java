package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.R;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.util.SecurityManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BudgetManager {
    private static BudgetManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private BudgetManager(Context context) {
        prefs = SecurityManager.getEncryptedPrefs(context, "pref_budget_plan");
        gson = new Gson();
    }

    public static synchronized BudgetManager getInstance(Context context) {
        if (instance == null) instance = new BudgetManager(context.getApplicationContext());
        return instance;
    }

    public void saveBudgetPlan(List<BudgetCategoryItem> items) {
        prefs.edit().putString("key_budget_plan_items", gson.toJson(items)).apply();
        notifyListeners();
    }

    public List<BudgetCategoryItem> getBudgetPlan() {
        String json = prefs.getString("key_budget_plan_items", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<BudgetCategoryItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void updateOrAddCategory(String name, double amount, String parent) {
        List<BudgetCategoryItem> items = getBudgetPlan();
        boolean found = false;
        for (BudgetCategoryItem item : items) {
            if (item.getCategoryName().equalsIgnoreCase(name)) {
                item.setAmount(amount);
                found = true;
                break;
            }
        }
        if (!found) {
            items.add(new BudgetCategoryItem(name, R.drawable.ic_chart, R.color.carti_primary_green, R.color.mint_green_alpha, amount, 0, parent, 0));
        }
        saveBudgetPlan(items);
    }

    public void addExpenseToCategory(String name, double spent) {
        List<BudgetCategoryItem> items = getBudgetPlan();
        for (BudgetCategoryItem item : items) {
            if (item.getCategoryName().equalsIgnoreCase(name)) {
                item.setCurrentSpent(item.getCurrentSpent() + spent);
                break;
            }
        }
        saveBudgetPlan(items);
    }

    public interface OnBudgetChangeListener { void onBudgetChanged(List<BudgetCategoryItem> items); }
    private final List<OnBudgetChangeListener> listeners = new ArrayList<>();
    public void addListener(OnBudgetChangeListener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(OnBudgetChangeListener l) { listeners.remove(l); }
    private void notifyListeners() {
        List<BudgetCategoryItem> current = getBudgetPlan();
        for (OnBudgetChangeListener l : listeners) l.onBudgetChanged(current);
    }
}
