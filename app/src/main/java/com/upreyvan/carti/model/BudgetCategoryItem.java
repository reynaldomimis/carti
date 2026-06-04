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

    private String parentCategory;
    private double currentSpent;
    private boolean isRecurring;
    private String iconUrl;

    public BudgetCategoryItem(String categoryName, int iconRes, int iconColor, int bgColor, double amount, int percentage) {
        this(categoryName, iconRes, iconColor, bgColor, amount, percentage, null, 0, false);
    }

    public BudgetCategoryItem(String categoryName, int iconRes, int iconColor, int bgColor, double amount, int percentage, String parentCategory, double currentSpent, boolean isRecurring) {
        this.categoryName = categoryName;
        this.iconRes = iconRes;
        this.iconColor = iconColor;
        this.bgColor = bgColor;
        this.amount = amount;
        this.percentage = percentage;
        this.parentCategory = parentCategory;
        this.currentSpent = currentSpent;
        this.isRecurring = isRecurring;
    }

    public String getCategoryName() { return categoryName; }
    public int getIconRes() { return iconRes; }
    public int getIconColor() { return iconColor; }
    public int getBgColor() { return bgColor; }
    public double getAmount() { return amount; }
    public int getPercentage() { return percentage; }
    public String getParentCategory() { return parentCategory; }
    public double getCurrentSpent() { return currentSpent; }
    public boolean isRecurring() { return isRecurring; }
    public String getIconUrl() { return iconUrl; }

    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }
    public void setBgColor(int bgColor) { this.bgColor = bgColor; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCurrentSpent(double spent) { this.currentSpent = spent; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public static final DiffUtil.ItemCallback<BudgetCategoryItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<BudgetCategoryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull BudgetCategoryItem oldItem, @NonNull BudgetCategoryItem newItem) {
            return oldItem.getCategoryName().equals(newItem.getCategoryName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BudgetCategoryItem oldItem, @NonNull BudgetCategoryItem newItem) {
            return oldItem.getAmount() == newItem.getAmount() && 
                   oldItem.getPercentage() == newItem.getPercentage() &&
                   oldItem.isRecurring() == newItem.isRecurring();
        }
    };
}
