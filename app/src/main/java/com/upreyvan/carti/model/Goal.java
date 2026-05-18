package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "goals")
public class Goal {
    @PrimaryKey
    @NonNull
    private String id;
    private String familyId;
    private String title;
    private String description;
    private double currentAmount;
    private double targetAmount;
    private String targetDate;
    private int imageRes;
    private int backgroundColor;

    @Ignore
    public Goal() {
        this.id = "";
        this.familyId = "";
        this.title = "Title";
        this.description = "Description";
    }

    public Goal(@NonNull String id, String familyId, String title, String description, double currentAmount, double targetAmount, String targetDate, int imageRes, int backgroundColor) {
        this.id = id;
        this.familyId = familyId;
        this.title = title;
        this.description = description;
        this.currentAmount = currentAmount;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.imageRes = imageRes;
        this.backgroundColor = backgroundColor;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public int getImageRes() { return imageRes; }
    public void setImageRes(int imageRes) { this.imageRes = imageRes; }
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }

    public boolean isCompleted() {
        return currentAmount >= targetAmount && targetAmount > 0;
    }

    public int getProgress() {
        if (targetAmount == 0) return 0;
        return (int) ((currentAmount / targetAmount) * 100);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Goal goal = (Goal) o;
        return Double.compare(goal.currentAmount, currentAmount) == 0 &&
                Double.compare(goal.targetAmount, targetAmount) == 0 &&
                id.equals(goal.id) &&
                Objects.equals(title, goal.title) &&
                Objects.equals(description, goal.description) &&
                Objects.equals(targetDate, goal.targetDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, currentAmount, targetAmount, targetDate);
    }

    public static final DiffUtil.ItemCallback<Goal> DIFF_CALLBACK = new DiffUtil.ItemCallback<Goal>() {
        @Override
        public boolean areItemsTheSame(@NonNull Goal oldItem, @NonNull Goal newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Goal oldItem, @NonNull Goal newItem) {
            return oldItem.equals(newItem);
        }
    };
}
