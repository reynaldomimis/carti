package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.util.Constants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GoalManager {
    private static GoalManager instance;
    private final List<Goal> goals;
    private final SharedPreferences prefs;
    private final Gson gson;
    private OnGoalChangeListener listener;

    public interface OnGoalChangeListener {
        void onGoalsUpdated();
    }

    private GoalManager(Context context) {
        prefs = context.getSharedPreferences(Constants.Keys.PREF_GOAL, Context.MODE_PRIVATE);
        gson = new Gson();
        goals = loadGoals();
    }

    public static synchronized GoalManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("GoalManager must be initialized with Context first");
        }
        return instance;
    }

    public static synchronized GoalManager init(Context context) {
        if (instance == null) {
            instance = new GoalManager(context.getApplicationContext());
        }
        return instance;
    }

    private List<Goal> loadGoals() {
        String json = prefs.getString(Constants.Keys.KEY_GOALS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<Goal>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveGoals() {
        prefs.edit().putString(Constants.Keys.KEY_GOALS, gson.toJson(goals)).apply();
    }

    public void setOnGoalChangeListener(OnGoalChangeListener listener) {
        this.listener = listener;
    }

    public void addGoal(Goal goal) {
        goals.add(0, goal);
        saveGoals();
        if (listener != null) {
            listener.onGoalsUpdated();
        }
    }

    public void setGoals(List<Goal> newGoals) {
        goals.clear();
        goals.addAll(newGoals);
        saveGoals();
        if (listener != null) {
            listener.onGoalsUpdated();
        }
    }

    public List<Goal> getGoals() {
        return new ArrayList<>(goals);
    }

    public void clear() {
        goals.clear();
        saveGoals();
        if (listener != null) {
            listener.onGoalsUpdated();
        }
    }
}
