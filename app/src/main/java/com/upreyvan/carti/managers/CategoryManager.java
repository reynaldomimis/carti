package com.upreyvan.carti.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.R;
import com.upreyvan.carti.models.Category;
import com.upreyvan.carti.utils.Constants;
import com.upreyvan.carti.utils.SecurityManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryManager {
    private static CategoryManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context context;
    private final List<OnCategoryUpdatedListener> listeners = new ArrayList<>();

    public interface OnCategoryUpdatedListener {
        void onCategoriesChanged();
    }

    public void addListener(OnCategoryUpdatedListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(OnCategoryUpdatedListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (OnCategoryUpdatedListener listener : new ArrayList<>(listeners)) {
            listener.onCategoriesChanged();
        }
    }

    private CategoryManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = SecurityManager.getEncryptedPrefs(context, Constants.Keys.PREF_CATEGORY);
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
        List<Category> categories;
        if (json == null) {
            categories = getDefaultCategories();
            saveCategories(categories);
            PreferenceManager.getInstance(context).resetAllSyncTimestamps();
        } else {
            Type type = new TypeToken<ArrayList<Category>>() {}.getType();
            categories = gson.fromJson(json, type);
            
            boolean needsSave = false;
            
            // Migration: Ensure "Bills" exists
            Category billsParent = null;
            for (Category c : categories) {
                if ("Bills".equalsIgnoreCase(c.getName())) {
                    billsParent = c;
                    break;
                }
            }
            if (billsParent == null) {
                billsParent = new Category("10", "Bills", R.drawable.ic_calendar, R.color.icon_electricity, R.color.log_electricity, true);
                categories.add(billsParent);
                needsSave = true;
            }

            // Migration: Ensure ALL 20 default Bill subcategories exist and use Parent's icon AND COLOR
            String[] billSubs = {"Water", "Electricity", "Internet/Wifi", "Rent", "Load/Data", "Cable TV", "Subscription", "Credit Card", "Insurance", "Tuition", "Home Dues", "Gym", "Installment", "Garbage", "Landline", "LPG", "Vehicle Loan", "PhilHealth", "Netflix", "Spotify"};
            for (String subName : billSubs) {
                boolean found = false;
                for (Category c : categories) {
                    if (c.getName().equalsIgnoreCase(subName)) {
                        if (!"Bills".equalsIgnoreCase(c.getParentCategory())) {
                            c.setParentCategory("Bills");
                            needsSave = true;
                        }
                        // FORCE sub-category to match parent's color and icon for synchronization
                        if (c.getIconColor() != billsParent.getIconColor() || c.getIconRes() != billsParent.getIconRes()) {
                            c.setIconRes(billsParent.getIconRes());
                            c.setIconColor(billsParent.getIconColor());
                            c.setBackgroundColor(billsParent.getBackgroundColor());
                            needsSave = true;
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    categories.add(new Category("sub_" + subName.toLowerCase().replace("/", "_"), subName, billsParent.getIconRes(), billsParent.getIconColor(), billsParent.getBackgroundColor(), true, "Bills"));
                    needsSave = true;
                }
            }

            // Migration: Ensure ALL 10 default Others subcategories exist and match Parent
            Category othersParent = null;
            for (Category c : categories) {
                if ("Others".equalsIgnoreCase(c.getName()) && (c.getParentCategory() == null || c.getParentCategory().isEmpty())) {
                    othersParent = c;
                    break;
                }
            }
            if (othersParent != null) {
                String[] otherSubs = {"Allowance", "Church/Donation", "Gifts/Celebration", "Laundry", "Household Help", "Miscellaneous", "Emergency", "Business/Side Hustle", "Special Occasion", "Tithe/Abuloy"};
                for (String subName : otherSubs) {
                    boolean found = false;
                    for (Category c : categories) {
                        if (c.getName().equalsIgnoreCase(subName)) {
                            if (!"Others".equalsIgnoreCase(c.getParentCategory())) {
                                c.setParentCategory("Others");
                                needsSave = true;
                            }
                            if (c.getIconColor() != othersParent.getIconColor() || c.getIconRes() != othersParent.getIconRes()) {
                                c.setIconRes(othersParent.getIconRes());
                                c.setIconColor(othersParent.getIconColor());
                                c.setBackgroundColor(othersParent.getBackgroundColor());
                                needsSave = true;
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        categories.add(new Category("sub_other_" + subName.toLowerCase().replace("/", "_"), subName, othersParent.getIconRes(), othersParent.getIconColor(), othersParent.getBackgroundColor(), true, "Others"));
                        needsSave = true;
                    }
                }
            }

            // Migration: Ensure Family Main Categories exist
            Object[][] familyMain = {
                {"f1", "Education", android.R.drawable.ic_menu_edit, R.color.icon_fare, R.color.log_fare}, 
                {"f2", "Personal Care", android.R.drawable.ic_menu_myplaces, R.color.icon_load, R.color.log_load}, 
                {"f3", "Shopping", android.R.drawable.ic_input_add, R.color.icon_store, R.color.log_store}, 
                {"f4", "Home Repair", android.R.drawable.ic_menu_manage, R.color.icon_others, R.color.log_others}, 
                {"f5", "Entertainment", android.R.drawable.ic_menu_slideshow, R.color.purple, R.color.tonal_button_bg}, 
                {"f6", "Pets", android.R.drawable.ic_menu_view, R.color.icon_food, R.color.log_food}, 
                {"f7", "Savings", android.R.drawable.ic_menu_save, R.color.carti_primary_green, R.color.mint_green_alpha}
            };
            for (Object[] main : familyMain) {
                boolean found = false;
                for (Category c : categories) {
                    if (c.getName().equalsIgnoreCase((String)main[1])) {
                        if (c.getIconRes() == R.drawable.ic_chart || c.getIconColor() == R.color.icon_others || c.getIconColor() == 0) {
                            c.setIconRes((Integer)main[2]);
                            c.setIconColor((Integer)main[3]);
                            c.setBackgroundColor((Integer)main[4]);
                            needsSave = true;
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    categories.add(new Category((String)main[0], (String)main[1], (Integer)main[2], (Integer)main[3], (Integer)main[4], true));
                    needsSave = true;
                }
            }

            if (needsSave) {
                saveCategories(categories);
            }
        }
        return sortCategories(categories);
    }

    public void addCategory(Category category) {
        List<Category> categories = getCategories();
        categories.add(category);
        saveCategories(categories);
        notifyListeners();
    }

    public void updateCategory(String oldName, Category updated) {
        List<Category> categories = getCategories();
        boolean isParent = updated.getParentCategory() == null || updated.getParentCategory().isEmpty();
        
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getName().equalsIgnoreCase(oldName)) {
                categories.set(i, updated);
                
                // If this is a parent category being updated, sync its children's style
                if (isParent) {
                    for (Category c : categories) {
                        if (updated.getName().equalsIgnoreCase(c.getParentCategory())) {
                            c.setIconRes(updated.getIconRes());
                            c.setIconColor(updated.getIconColor());
                            c.setBackgroundColor(updated.getBackgroundColor());
                        }
                    }
                }

                saveCategories(categories);
                notifyListeners();
                return;
            }
        }
        addCategory(updated);
    }

    public void deleteCategory(String name) {
        List<Category> categories = getCategories();
        categories.removeIf(c -> c.getName().equalsIgnoreCase(name) || name.equalsIgnoreCase(c.getParentCategory()));
        saveCategories(categories);
        notifyListeners();
    }

    public void updateCategories(List<Category> categories) {
        saveCategories(categories);
    }

    public Category getCategoryByName(String name) {
        if (name == null) return null;
        for (Category c : getCategories()) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    private void saveCategories(List<Category> categories) {
        String json = gson.toJson(categories);
        prefs.edit().putString(Constants.Keys.KEY_CATEGORIES + "_v2", json).apply();
    }

    private List<Category> sortCategories(List<Category> list) {
        if (list == null) return new ArrayList<>();
        List<Category> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> {
            boolean aOther = "others".equalsIgnoreCase(a.getName());
            boolean bOther = "others".equalsIgnoreCase(b.getName());
            if (aOther && bOther) return 0;
            if (aOther) return 1;
            if (bOther) return -1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return sorted;
    }

    private List<Category> getDefaultCategories() {
        List<Category> defaults = new ArrayList<>();
        defaults.add(new Category("1", "Food", android.R.drawable.ic_menu_gallery, R.color.icon_food, R.color.log_food, true));
        defaults.add(new Category("2", "Transport", android.R.drawable.ic_dialog_map, R.color.icon_fare, R.color.log_fare, true));
        defaults.add(new Category("3", "Grocery", android.R.drawable.ic_input_add, R.color.icon_others, R.color.log_others, true));
        defaults.add(new Category("5", "Sari-sari", android.R.drawable.ic_menu_agenda, R.color.icon_store, R.color.log_store, true));
        defaults.add(new Category("6", "Health", android.R.drawable.ic_menu_compass, R.color.status_red, R.color.status_red_tonal, true));
        defaults.add(new Category("7", "Debt/Utang", android.R.drawable.ic_lock_lock, R.color.icon_debt, R.color.log_debt, true));
        
        // Family-oriented Main Categories
        defaults.add(new Category("f1", "Education", android.R.drawable.ic_menu_edit, R.color.icon_fare, R.color.log_fare, true));
        defaults.add(new Category("f2", "Personal Care", android.R.drawable.ic_menu_myplaces, R.color.icon_load, R.color.log_load, true));
        defaults.add(new Category("f3", "Shopping", android.R.drawable.ic_input_add, R.color.icon_store, R.color.log_store, true));
        defaults.add(new Category("f4", "Home Repair", android.R.drawable.ic_menu_manage, R.color.icon_others, R.color.log_others, true));
        defaults.add(new Category("f5", "Entertainment", android.R.drawable.ic_menu_slideshow, R.color.purple, R.color.tonal_button_bg, true));
        defaults.add(new Category("f6", "Pets", android.R.drawable.ic_menu_view, R.color.icon_food, R.color.log_food, true));
        defaults.add(new Category("f7", "Savings", android.R.drawable.ic_menu_save, R.color.carti_primary_green, R.color.mint_green_alpha, true));

        // Bills Main Category
        Category bills = new Category("10", "Bills", R.drawable.ic_calendar, R.color.icon_electricity, R.color.log_electricity, true);
        defaults.add(bills);
        
        // Bills Subcategories - Use parent icon and colors
        String[] billSubs = {"Water", "Electricity", "Internet/Wifi", "Rent", "Load/Data", "Cable TV", "Subscription", "Credit Card", "Insurance", "Tuition", "Home Dues", "Gym", "Installment", "Garbage", "Landline", "LPG", "Vehicle Loan", "PhilHealth", "Netflix", "Spotify"};
        for (int i = 0; i < billSubs.length; i++) {
            defaults.add(new Category("10" + i, billSubs[i], bills.getIconRes(), bills.getIconColor(), bills.getBackgroundColor(), true, "Bills"));
        }

        // Others Main Category and subcategories
        Category others = new Category("8", "Others", android.R.drawable.ic_menu_more, R.color.icon_others, R.color.log_others, true);
        defaults.add(others);
        String[] otherSubs = {"Allowance", "Church/Donation", "Gifts/Celebration", "Laundry", "Household Help", "Miscellaneous", "Emergency", "Business/Side Hustle", "Special Occasion", "Tithe/Abuloy"};
        for (int i = 0; i < otherSubs.length; i++) {
            defaults.add(new Category("80" + i, otherSubs[i], others.getIconRes(), others.getIconColor(), others.getBackgroundColor(), true, "Others"));
        }

        return defaults;
    }
}
