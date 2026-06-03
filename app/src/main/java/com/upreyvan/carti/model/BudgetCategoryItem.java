package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class BudgetCategoryItem {
    private String categoryName;
    private int iconRes;
    private int iconColor;
    private int bgColor;
    private double amount;
    private int percentage;

    private String parentCategory; // For Elite Hierarchy
    private double currentSpent; // For actual vs budget tracking

    public BudgetCategoryItem(String categoryName, int iconRes, int iconColor, int bgColor, double amount, int percentage) {
        this(categoryName, iconRes, iconColor, bgColor, amount, percentage, null, 0);
    }

    public BudgetCategoryItem(String categoryName, int iconRes, int iconColor, int bgColor, double amount, int percentage, String parentCategory, double currentSpent) {
        this.categoryName = categoryName;
        this.iconRes = iconRes;
        this.iconColor = iconColor;
        this.bgColor = bgColor;
        this.amount = amount;
        this.percentage = percentage;
        this.parentCategory = parentCategory;
        this.currentSpent = currentSpent;
    }

    public String getCategoryName() { return categoryName; }
    public int getIconRes() { return iconRes; }
    public int getIconColor() { return iconColor; }
    public int getBgColor() { return bgColor; }
    public double getAmount() { return amount; }
    public int getPercentage() { return percentage; }
    public String getParentCategory() { return parentCategory; }
    public double getCurrentSpent() { return currentSpent; }

    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }
    public void setBgColor(int bgColor) { this.bgColor = bgColor; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCurrentSpent(double spent) { this.currentSpent = spent; }

    public static final DiffUtil.ItemCallback<BudgetCategoryItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<BudgetCategoryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull BudgetCategoryItem oldItem, @NonNull BudgetCategoryItem newItem) {
            return oldItem.getCategoryName().equals(newItem.getCategoryName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BudgetCategoryItem oldItem, @NonNull BudgetCategoryItem newItem) {
            return oldItem.getAmount() == newItem.getAmount() && oldItem.getPercentage() == newItem.getPercentage();
        }
    };
}
