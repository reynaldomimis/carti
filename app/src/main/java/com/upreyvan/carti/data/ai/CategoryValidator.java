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

        if (SYSTEM_CATEGORIES.contains(currentCategory)) {
            callback.onValidated(currentCategory);
            return;
        }

        String localRemap = CategoryMapper.map(context, item);
        if (localRemap != null && SYSTEM_CATEGORIES.contains(localRemap)) {
            callback.onValidated(localRemap);
            return;
        }

        callback.onValidated("Others");
    }
}
