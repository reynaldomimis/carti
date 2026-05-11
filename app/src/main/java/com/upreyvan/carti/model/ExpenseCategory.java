package com.upreyvan.carti.model;

public class ExpenseCategory {
    private String name;
    private double amount;
    private float percentage;
    private int color;

    public ExpenseCategory(String name, double amount, float percentage, int color) {
        this.name = name;
        this.amount = amount;
        this.percentage = percentage;
        this.color = color;
    }

    public String getName() { return name; }
    public double getAmount() { return amount; }
    public float getPercentage() { return percentage; }
    public int getColor() { return color; }
}