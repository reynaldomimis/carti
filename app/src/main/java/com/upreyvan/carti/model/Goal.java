package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "goals")
public class Goal {
    @PrimaryKey
    @NonNull
    private String id;
    private String familyId;
    private String title;
    private double currentAmount;
    private double targetAmount;
    private String targetDate;
    private int imageRes;
    private int backgroundColor;

    @Ignore
    public Goal() {
        this.id = "";
        this.familyId = "";
        this.title = "";
    }

    public Goal(@NonNull String id, String familyId, String title, double currentAmount, double targetAmount, String targetDate, int imageRes, int backgroundColor) {
        this.id = id;
        this.familyId = familyId;
        this.title = title;
        this.currentAmount = currentAmount;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.imageRes = imageRes;
        this.backgroundColor = backgroundColor;
    }

    @NonNull
    public String getId() { return id; }
    public String getFamilyId() { return familyId; }
    public void setId(@NonNull String id) { this.id = id; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public void setTitle(String title) { this.title = title; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public void setImageRes(int imageRes) { this.imageRes = imageRes; }
    public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }

    public boolean isCompleted() {
        return currentAmount >= targetAmount && targetAmount > 0;
    }

    public String getTitle() { return title; }
    public double getCurrentAmount() { return currentAmount; }
    public double getTargetAmount() { return targetAmount; }
    public String getTargetDate() { return targetDate; }
    public int getImageRes() { return imageRes; }
    public int getBackgroundColor() { return backgroundColor; }

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
                java.util.Objects.equals(title, goal.title) &&
                java.util.Objects.equals(targetDate, goal.targetDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, title, currentAmount, targetAmount, targetDate);
    }

    public static final androidx.recyclerview.widget.DiffUtil.ItemCallback<Goal> DIFF_CALLBACK =
            new androidx.recyclerview.widget.DiffUtil.ItemCallback<Goal>() {
                @Override
                public boolean areItemsTheSame(@androidx.annotation.NonNull Goal oldItem, @androidx.annotation.NonNull Goal newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@androidx.annotation.NonNull Goal oldItem, @androidx.annotation.NonNull Goal newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
