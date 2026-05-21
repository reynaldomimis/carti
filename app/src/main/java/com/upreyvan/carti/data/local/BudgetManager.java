package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.util.Constants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BudgetManager {
    private static BudgetManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private BudgetManager(Context context) {
        prefs = context.getSharedPreferences("pref_budget_plan", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized BudgetManager getInstance(Context context) {
        if (instance == null) {
            instance = new BudgetManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveBudgetPlan(List<BudgetCategoryItem> items) {
        String json = gson.toJson(items);
        prefs.edit().putString("key_budget_plan_items", json).apply();
    }

    public List<BudgetCategoryItem> getBudgetPlan() {
        String json = prefs.getString("key_budget_plan_items", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<BudgetCategoryItem>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
