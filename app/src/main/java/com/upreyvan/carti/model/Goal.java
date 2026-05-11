package com.upreyvan.carti.model;

public class Goal {
    private String title;
    private double currentAmount;
    private double targetAmount;
    private String targetDate;
    private int imageRes;
    private int backgroundColor;

    public Goal(String title, double currentAmount, double targetAmount, String targetDate, int imageRes, int backgroundColor) {
        this.title = title;
        this.currentAmount = currentAmount;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.imageRes = imageRes;
        this.backgroundColor = backgroundColor;
    }

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
}
