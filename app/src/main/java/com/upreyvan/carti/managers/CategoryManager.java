package com.upreyvan.carti.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.R;
import com.upreyvan.carti.datasource.ApiHelper;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.models.Category;
import com.upreyvan.carti.utils.Constants;
import com.upreyvan.carti.utils.SecurityManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
        List<Category> categories = new ArrayList<>();
        
        if (json != null) {
            Type type = new TypeToken<ArrayList<Category>>() {}.getType();
            categories = gson.fromJson(json, type);
        }

        return sortCategories(categories);
    }

    public void addCategory(Category category) {
        List<Category> categories = getCategories();
        categories.add(category);
        saveCategories(categories);
        notifyListeners();
    }

    public void updateCategory(String id, Category updated) {
        List<Category> categories = getCategories();
        boolean isParent = updated.getParentCategory() == null || updated.getParentCategory().isEmpty();
        boolean found = false;

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(id)) {
                categories.set(i, updated);
                found = true;
                
                if (isParent) {
                    for (Category c : categories) {
                        if (updated.getName().equalsIgnoreCase(c.getParentCategory())) {
                            c.setIconRes(updated.getIconRes());
                            c.setIconColor(updated.getIconColor());
                            c.setBackgroundColor(updated.getBackgroundColor());
                        }
                    }
                }
                break;
            }
        }
        
        if (!found) {
            categories.add(updated);
        }
        
        saveCategories(categories);
        notifyListeners();
    }

    public void deleteCategory(String id) {
        List<Category> categories = getCategories();
        Category toDelete = getCategoryById(id);
        if (toDelete != null) {
            String name = toDelete.getName();
            categories.removeIf(c -> c.getId().equals(id) || name.equalsIgnoreCase(c.getParentCategory()));
            saveCategories(categories);
            notifyListeners();
        }
    }

    public void updateCategories(List<Category> categories) {
        saveCategories(categories);
        notifyListeners();
    }

    public void refreshRemoteCategories(String familyId) {
        AppwriteManager.getInstance(context).listDocuments(
                Constants.Appwrite.DATABASE_ID,
                Constants.Appwrite.COL_CATEGORIES,
                java.util.Arrays.asList(
                        io.appwrite.Query.Companion.equal("familyId", java.util.Arrays.asList(familyId, "system"))
                ),
                new AppwriteManager.AppwriteCallback<io.appwrite.models.DocumentList<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(io.appwrite.models.DocumentList<Map<String, Object>> result) {
                        List<Category> remoteList = new ArrayList<>();
                        for (io.appwrite.models.Document<Map<String, Object>> doc : result.getDocuments()) {
                            remoteList.add(mapToCategory(doc.getId(), doc.getData()));
                        }
                        if (!remoteList.isEmpty()) {
                            updateCategories(remoteList);
                        }
                    }

                    @Override public void onError(Throwable error) { android.util.Log.e("CategoryManager", "Refresh failed: " + error.getMessage()); }
                }
        );
    }

    private Category mapToCategory(String id, Map<String, Object> data) {
        String name = (String) data.get("name");
        int iconRes = ((Number) data.getOrDefault("iconRes", 0)).intValue();
        int iconColor = ((Number) data.getOrDefault("iconColor", 0)).intValue();
        int bgColor = ((Number) data.getOrDefault("backgroundColor", 0)).intValue();
        String parent = (String) data.get("parentCategory");
        return new Category(id, name, iconRes, iconColor, bgColor, false, parent);
    }

    public void deleteCategoryRemote(String categoryId) {
        ApiHelper helper = new ApiHelper(context);
        helper.deleteCategory(categoryId, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) { refreshRemoteCategories(PreferenceManager.getInstance(context).getFamilyId()); }
            @Override public void onError(Throwable error) {}
        });
    }

    public void updateCategoryRemote(String categoryId, Category category, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        ApiHelper helper = new ApiHelper(context);
        Map<String, Object> data = new HashMap<>();
        data.put("name", category.getName());
        data.put("iconRes", category.getIconRes());
        data.put("iconColor", category.getIconColor());
        data.put("backgroundColor", category.getBackgroundColor());
        data.put("parentCategory", category.getParentCategory());
        
        helper.updateCategory(categoryId, data, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> result) { 
                refreshRemoteCategories(PreferenceManager.getInstance(context).getFamilyId());
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public List<Map<String, Object>> getCategoriesForSync(String familyId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Category c : getCategories()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", c.getName());
            map.put("iconRes", c.getIconRes());
            map.put("iconColor", c.getIconColor());
            map.put("backgroundColor", c.getBackgroundColor());
            map.put("parentCategory", c.getParentCategory());
            map.put("familyId", familyId);
            list.add(map);
        }
        return list;
    }

    public void addCategoryRemote(Category category, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("name", category.getName());
        map.put("iconRes", category.getIconRes());
        map.put("iconColor", category.getIconColor());
        map.put("backgroundColor", category.getBackgroundColor());
        map.put("parentCategory", category.getParentCategory());
        map.put("familyId", PreferenceManager.getInstance(context).getFamilyId());
        list.add(map);

        ApiHelper helper = new ApiHelper(context);
        helper.syncCategories(list, callback);
    }

    public void syncAllToRemote(String familyId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        ApiHelper helper = new ApiHelper(context);
        helper.syncCategories(getCategoriesForSync(familyId), callback);
    }

    public Category getCategoryByName(String name) {
        if (name == null) return null;
        for (Category c : getCategories()) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    public Category getCategoryById(String id) {
        if (id == null) return null;
        for (Category c : getCategories()) {
            if (id.equals(c.getId())) return c;
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
}
