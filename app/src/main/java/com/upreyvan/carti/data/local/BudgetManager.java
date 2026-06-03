package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
    private final MutableLiveData<List<BudgetCategoryItem>> budgetPlanLiveData = new MutableLiveData<>();

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
        budgetPlanLiveData.postValue(items);
    }

    public LiveData<List<BudgetCategoryItem>> getBudgetPlanLiveData() {
        if (budgetPlanLiveData.getValue() == null) {
            budgetPlanLiveData.postValue(getBudgetPlan());
        }
        return budgetPlanLiveData;
    }

    public List<BudgetCategoryItem> getBudgetPlan() {
        String json = prefs.getString("key_budget_plan_items", null);
        if (json == null) return getDefaultCategories();
        Type type = new TypeToken<ArrayList<BudgetCategoryItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public List<BudgetCategoryItem> getDefaultCategories() {
        List<BudgetCategoryItem> items = new ArrayList<>();
        items.add(new BudgetCategoryItem("Food", R.drawable.ic_chart, R.color.icon_food, R.color.log_food, 0, 0));
        items.add(new BudgetCategoryItem("Transportation", R.drawable.ic_chart, R.color.icon_fare, R.color.log_fare, 0, 0));
        items.add(new BudgetCategoryItem("Bills", R.drawable.ic_calendar, R.color.icon_water, R.color.log_water, 0, 0));
        items.add(new BudgetCategoryItem("Shopping", R.drawable.ic_chart, R.color.icon_store, R.color.log_store, 0, 0));
        items.add(new BudgetCategoryItem("Health", R.drawable.ic_chart, R.color.status_red, R.color.status_red_tonal, 0, 0));
        items.add(new BudgetCategoryItem("Education", R.drawable.ic_chart, R.color.carti_primary_blue, R.color.log_fare, 0, 0));
        items.add(new BudgetCategoryItem("Others", R.drawable.ic_chart, R.color.icon_others, R.color.log_others, 0, 0));
        return items;
    }

    public void updateOrAddCategory(String name, double amount, String parent) {
        updateOrAddCategory(name, R.drawable.ic_chart, R.color.carti_primary_green, R.color.mint_green_alpha, amount, parent);
    }

    public void updateOrAddCategory(String name, int icon, int iconColor, int bgColor, double amount, String parent) {
        List<BudgetCategoryItem> items = getBudgetPlan();
        boolean found = false;
        for (BudgetCategoryItem item : items) {
            if (item.getCategoryName().equalsIgnoreCase(name)) {
                item.setAmount(amount);
                item.setIconRes(icon);
                item.setIconColor(iconColor);
                item.setBgColor(bgColor);
                found = true;
                break;
            }
        }
        if (!found) {
            BudgetCategoryItem def = null;
            for (BudgetCategoryItem d : getDefaultCategories()) {
                if (d.getCategoryName().equalsIgnoreCase(name)) { def = d; break; }
            }
            
            if (def != null) {
                def.setAmount(amount);
                if (icon != R.drawable.ic_chart) {
                    def.setIconRes(icon);
                    def.setIconColor(iconColor);
                    def.setBgColor(bgColor);
                }
                items.add(def);
            } else {
                items.add(new BudgetCategoryItem(name, icon, iconColor, bgColor, amount, 0, parent, 0));
            }
        }
        saveBudgetPlan(items);
    }
}
