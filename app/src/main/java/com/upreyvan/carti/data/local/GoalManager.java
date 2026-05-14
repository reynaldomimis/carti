package com.upreyvan.carti.data.local;

import com.upreyvan.carti.model.Goal;
import java.util.ArrayList;
import java.util.List;

public class GoalManager {
    private static GoalManager instance;
    private final List<Goal> goals;
    private OnGoalChangeListener listener;

    public interface OnGoalChangeListener {
        void onGoalsUpdated();
    }

    private GoalManager() {
        goals = new ArrayList<>();
    }

    public static synchronized GoalManager getInstance() {
        if (instance == null) {
            instance = new GoalManager();
        }
        return instance;
    }

    public void setOnGoalChangeListener(OnGoalChangeListener listener) {
        this.listener = listener;
    }

    public void setGoals(List<Goal> newGoals) {
        goals.clear();
        goals.addAll(newGoals);
        if (listener != null) {
            listener.onGoalsUpdated();
        }
    }

    public List<Goal> getGoals() {
        return new ArrayList<>(goals);
    }

    public void clear() {
        goals.clear();
        if (listener != null) {
            listener.onGoalsUpdated();
        }
    }
}
