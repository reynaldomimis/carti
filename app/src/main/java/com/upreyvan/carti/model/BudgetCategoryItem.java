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

    public BudgetCategoryItem(String categoryName, int iconRes, int iconColor, int bgColor, double amount, int percentage) {
        this.categoryName = categoryName;
        this.iconRes = iconRes;
        this.iconColor = iconColor;
        this.bgColor = bgColor;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getCategoryName() { return categoryName; }
    public int getIconRes() { return iconRes; }
    public int getIconColor() { return iconColor; }
    public int getBgColor() { return bgColor; }
    public double getAmount() { return amount; }
    public int getPercentage() { return percentage; }

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
