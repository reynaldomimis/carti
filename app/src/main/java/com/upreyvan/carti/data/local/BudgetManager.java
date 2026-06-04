package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.RecurringBudgetStats;
import com.upreyvan.carti.util.SecurityManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetManager {
    private static BudgetManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context context;
    private final MutableLiveData<List<BudgetCategoryItem>> budgetPlanLiveData = new MutableLiveData<>();
    private final MutableLiveData<RecurringBudgetStats> recurringStatsLiveData = new MutableLiveData<>();

    private BudgetManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = SecurityManager.getEncryptedPrefs(this.context, "pref_budget_plan");
        gson = new Gson();
    }

    public static synchronized BudgetManager getInstance(Context context) {
        if (instance == null) instance = new BudgetManager(context.getApplicationContext());
        return instance;
    }

    public LiveData<RecurringBudgetStats> getRecurringStatsLiveData() {
        refreshRecurringStats();
        return recurringStatsLiveData;
    }

    public void refreshRecurringStats() {
        List<BudgetCategoryItem> items = getBudgetPlan();
        int count = 0;
        double amount = 0;
        for (BudgetCategoryItem item : items) {
            if (item.isRecurring()) {
                count++;
                amount += item.getAmount();
            }
        }
        
        PreferenceManager pref = PreferenceManager.getInstance(context);
        String currentMonthKey = getCurrentMonthKey();
        boolean needsProcessing = !currentMonthKey.equals(pref.getLastRecurringCheck());
        
        recurringStatsLiveData.postValue(new RecurringBudgetStats(count, amount, pref.getLastRecurringCheck(), needsProcessing));
    }

    private String getCurrentMonthKey() {
        Calendar now = Calendar.getInstance();
        return String.format(Locale.US, "%d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
    }

    public void processRecurringBudgets() {
        PreferenceManager pref = PreferenceManager.getInstance(context);
        String currentMonthKey = getCurrentMonthKey();
        
        if (currentMonthKey.equals(pref.getLastRecurringCheck())) return;

        List<BudgetCategoryItem> items = getBudgetPlan();
        for (BudgetCategoryItem item : items) {
            if (item.isRecurring() && item.getAmount() > 0) {
                TransactionRepository.getInstance(context).addTransaction(
                    item.getAmount(), 
                    "INCOME", 
                    item.getCategoryName(), 
                    "Recurring: " + item.getCategoryName(), 
                    null
                );
            }
        }
        pref.setLastRecurringCheck(currentMonthKey);
        refreshRecurringStats();
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

    public void updateOrAddCategory(String name, double amount, String parent, boolean isRecurring) {
        updateOrAddCategory(name, R.drawable.ic_chart, R.color.carti_primary_green, R.color.mint_green_alpha, amount, parent, isRecurring);
    }

    public void updateOrAddCategory(String name, int icon, int iconColor, int bgColor, double amount, String parent, boolean isRecurring) {
        List<BudgetCategoryItem> items = getBudgetPlan();
        boolean found = false;
        for (BudgetCategoryItem item : items) {
            if (item.getCategoryName().equalsIgnoreCase(name)) {
                item.setAmount(amount);
                item.setIconRes(icon);
                item.setIconColor(iconColor);
                item.setBgColor(bgColor);
                item.setRecurring(isRecurring);
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
                def.setRecurring(isRecurring);
                if (icon != R.drawable.ic_chart) {
                    def.setIconRes(icon);
                    def.setIconColor(iconColor);
                    def.setBgColor(bgColor);
                }
                items.add(def);
            } else {
                items.add(new BudgetCategoryItem(name, icon, iconColor, bgColor, amount, 0, parent, 0, isRecurring));
            }
        }
        saveBudgetPlan(items);
    }
}
