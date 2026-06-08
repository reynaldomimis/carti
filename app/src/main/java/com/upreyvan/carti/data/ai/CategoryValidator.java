package com.upreyvan.carti.data.ai;

import android.content.Context;
import android.util.Log;
import com.upreyvan.carti.util.CategoryMapper;
import com.upreyvan.carti.data.repository.AiRepository;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CategoryValidator {
    private static final String TAG = "CategoryValidator";

    public static final List<String> SYSTEM_CATEGORIES = Arrays.asList(
        "Food", "Bills", "Transportation", "Shopping", "Personal Care", 
        "Health", "Education", "Work", "Income", "Others"
    );

    public interface ValidationCallback {
        void onValidated(String category);
    }

    public static void validate(Context context, String item, String currentCategory, ValidationCallback callback) {
        // 1. Get actual categories from Set Up Categories
        List<com.upreyvan.carti.model.Category> actualCats = com.upreyvan.carti.data.local.CategoryManager.getInstance(context).getCategories();
        List<String> validNames = new ArrayList<>();
        for (com.upreyvan.carti.model.Category c : actualCats) validNames.add(c.getName());

        // 2. Direct match with current category
        if (validNames.contains(currentCategory)) {
            callback.onValidated(currentCategory);
            return;
        }

        // 3. Use Mapper to find best match from actual categories
        String localRemap = CategoryMapper.map(context, item != null ? item : currentCategory);
        if (localRemap != null && validNames.contains(localRemap)) {
            callback.onValidated(localRemap);
            return;
        }

        // 4. Fallback to Others if it exists in actual categories
        if (validNames.contains("Others")) {
            callback.onValidated("Others");
        } else if (!validNames.isEmpty()) {
            callback.onValidated(validNames.get(validNames.size() - 1)); // Last resort
        } else {
            callback.onValidated(currentCategory);
        }
    }
}
