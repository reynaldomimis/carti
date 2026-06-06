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
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.SecurityManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public void saveBudgetPlanFromJson(String json) {
        if (json == null || json.isEmpty()) return;
        Type type = new TypeToken<ArrayList<BudgetCategoryItem>>() {}.getType();
        List<BudgetCategoryItem> items = gson.fromJson(json, type);
        if (items != null) saveBudgetPlan(items);
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
        items.add(new BudgetCategoryItem("Shopping", R.drawable.ic_chart, R.color.icon_store, R.color.log_store, 0, 0));
        items.add(new BudgetCategoryItem("Health", R.drawable.ic_chart, R.color.status_red, R.color.status_red_tonal, 0, 0));
        items.add(new BudgetCategoryItem("Others", R.drawable.ic_chart, R.color.icon_others, R.color.log_others, 0, 0));
        return items;
    }

    public List<BudgetCategoryItem> getConsolidatedBudgets(
            List<TransactionWithUser> dbAllocations,
            List<com.upreyvan.carti.data.local.db.dao.TransactionDao.CategorySum> expenses) {
        
        List<BudgetCategoryItem> basePlan = getBudgetPlan();
        
        Map<String, Double> budgetMap = new HashMap<>();
        if (dbAllocations != null) {
            for (TransactionWithUser tu : dbAllocations) {
                budgetMap.put(tu.getTransaction().getCategory().toLowerCase(Locale.ROOT), tu.getTransaction().getAmount());
            }
        }

        Map<String, Double> expenseMap = new HashMap<>();
        if (expenses != null) {
            for (com.upreyvan.carti.data.local.db.dao.TransactionDao.CategorySum e : expenses) {
                expenseMap.put(e.category.toLowerCase(Locale.ROOT), e.total);
            }
        }

        List<BudgetCategoryItem> consolidated = new ArrayList<>();
        for (BudgetCategoryItem item : basePlan) {
            String catName = item.getCategoryName();
            String catLower = catName.toLowerCase(Locale.ROOT);
            
            Double budgetLimit = budgetMap.get(catLower);
            double limit = (budgetLimit != null) ? budgetLimit : item.getAmount();
            
            Double totalSpent = expenseMap.get(catLower);
            double spent = (totalSpent != null) ? totalSpent : 0.0;

            int pct = limit > 0 ? (int)((spent / limit) * 100) : 0;
            
            consolidated.add(new BudgetCategoryItem(
                    catName, item.getIconRes(), item.getIconColor(), item.getBgColor(),
                    limit, pct, item.getParentCategory(), spent, item.isRecurring()
            ));
        }
        return consolidated;
    }

    public void updateOrAddCategory(String name, double amount, String parent, boolean isRecurring) {
        updateOrAddCategory(name, R.drawable.ic_chart, R.color.carti_primary_green, R.color.mint_green_alpha, amount, parent, isRecurring);
    }

    public void updateOrAddCategory(String name, int icon, int iconColor, int bgColor, double amount, String parent, boolean isRecurring) {
        updateOrAddCategory(null, name, icon, iconColor, bgColor, amount, parent, isRecurring);
    }

    public void updateOrAddCategory(String oldName, String name, int icon, int iconColor, int bgColor, double amount, String parent, boolean isRecurring) {
        List<BudgetCategoryItem> items = getBudgetPlan();
        String targetName = (oldName != null) ? oldName : name;
        BudgetCategoryItem foundItem = null;

        java.util.Iterator<BudgetCategoryItem> it = items.iterator();
        while (it.hasNext()) {
            BudgetCategoryItem item = it.next();
            if (item.getCategoryName().equalsIgnoreCase(targetName)) {
                if (foundItem == null) {
                    foundItem = item;
                    item.setCategoryName(name);
                    item.setAmount(amount);
                    item.setIconRes(icon);
                    item.setIconColor(iconColor);
                    item.setBgColor(bgColor);
                    item.setParentCategory(parent);
                    item.setRecurring(isRecurring);

                    if (oldName != null && !oldName.equalsIgnoreCase(name)) {
                        for (BudgetCategoryItem sub : items) {
                            if (oldName.equalsIgnoreCase(sub.getParentCategory())) {
                                sub.setParentCategory(name);
                            }
                        }
                    }
                } else {
                    it.remove();
                }
            }
        }

        if (foundItem == null) {
            BudgetCategoryItem def = null;
            for (BudgetCategoryItem d : getDefaultCategories()) {
                if (d.getCategoryName().equalsIgnoreCase(name)) { def = d; break; }
            }
            
            if (def != null) {
                def.setAmount(amount);
                def.setRecurring(isRecurring);
                def.setParentCategory(parent);
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

    public void deleteCategory(String name) {
        List<BudgetCategoryItem> items = getBudgetPlan();
        items.removeIf(item -> item.getCategoryName().equalsIgnoreCase(name) || (item.getParentCategory() != null && item.getParentCategory().equalsIgnoreCase(name)));
        saveBudgetPlan(items);
        budgetPlanLiveData.postValue(items);
    }
}
