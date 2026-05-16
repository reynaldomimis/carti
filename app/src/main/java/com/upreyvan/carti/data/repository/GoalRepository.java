package com.upreyvan.carti.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.GoalDao;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class GoalRepository {
    private final GoalDao goalDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GoalRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        goalDao = db.goalDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
    }

    public LiveData<List<Goal>> getAllGoals() {
        return goalDao.getAllGoals(pref.getFamilyId());
    }

    public void syncGoalsIfNeeded() {
        refreshGoals();
    }

    public void refreshGoals() {
        String familyId = pref.getFamilyId();
        String lastSync = pref.getLastGoalSyncTime();

        apiHelper.getGoalsSince(lastSync, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (result.getDocuments().isEmpty()) return;

                executor.execute(() -> {
                    List<Goal> goals = new ArrayList<>();
                    String latestTimestamp = lastSync;

                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        goals.add(mapToGoal(doc, familyId));
                        
                        if (doc.getCreatedAt().compareTo(latestTimestamp) > 0) {
                            latestTimestamp = doc.getCreatedAt();
                        }
                    }
                    
                    goalDao.insertAll(goals);
                    pref.setLastGoalSyncTime(latestTimestamp);
                });
            }

            @Override
            public void onError(Throwable error) {}
        });
    }

    public void saveLocally(Goal goal) {
        goal.setFamilyId(pref.getFamilyId());
        executor.execute(() -> goalDao.insert(goal));
    }

    public void deleteLocally(String id) {
        executor.execute(() -> goalDao.deleteById(id));
    }

    private Goal mapToGoal(Document<Map<String, Object>> doc, String familyId) {
        Map<String, Object> data = doc.getData();
        return new Goal(
            doc.getId(),
            familyId,
            String.valueOf(data.get("name")),
            Utils.getDouble(data.get("currentAmount")),
            Utils.getDouble(data.get("targetAmount")),
            "", // Date if needed
            R.drawable.ic_trophy, // Fixed resource ID
            R.color.carti_light_gray
        );
    }
