package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.R;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.util.Constants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryManager {
    private static CategoryManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context context;

    private CategoryManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(Constants.Keys.PREF_CATEGORY, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized CategoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryManager(context.getApplicationContext());
        }
        return instance;
    }

    public List<Category> getCategories() {
        String json = prefs.getString(Constants.Keys.KEY_CATEGORIES + "_v2", null);
        if (json == null) {
            List<Category> defaults = getDefaultCategories();
            saveCategories(defaults);
            PreferenceManager.getInstance(context).resetAllSyncTimestamps();
            return defaults;
        }
        Type type = new TypeToken<ArrayList<Category>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void addCategory(Category category) {
        List<Category> categories = getCategories();
        categories.add(category);
        saveCategories(categories);
    }

    public void updateCategories(List<Category> categories) {
        saveCategories(categories);
    }

    private void saveCategories(List<Category> categories) {
        String json = gson.toJson(categories);
        prefs.edit().putString(Constants.Keys.KEY_CATEGORIES + "_v2", json).apply();
    }

    private List<Category> getDefaultCategories() {
        List<Category> defaults = new ArrayList<>();
        // High frequency daily expenses
        defaults.add(new Category("1", "Food", android.R.drawable.ic_menu_gallery, R.color.icon_food, R.color.log_food, true));
        defaults.add(new Category("2", "Transport", android.R.drawable.ic_dialog_map, R.color.icon_fare, R.color.log_fare, true));
        defaults.add(new Category("3", "Grocery", android.R.drawable.ic_input_add, R.color.icon_others, R.color.log_others, true));
        defaults.add(new Category("4", "Load/Data", android.R.drawable.ic_menu_send, R.color.icon_load, R.color.log_load, true));
        defaults.add(new Category("5", "Sari-sari", android.R.drawable.ic_menu_agenda, R.color.icon_store, R.color.log_store, true));
        defaults.add(new Category("6", "Health", android.R.drawable.ic_menu_compass, R.color.status_red, R.color.status_red_tonal, true));
        defaults.add(new Category("7", "Debt/Utang", android.R.drawable.ic_lock_lock, R.color.icon_debt, R.color.log_debt, true));
        defaults.add(new Category("8", "Others", android.R.drawable.ic_menu_more, R.color.icon_others, R.color.log_others, true));
        return defaults;
    }
}
