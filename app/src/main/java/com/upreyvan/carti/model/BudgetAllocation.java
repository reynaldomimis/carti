package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.util.ArrayList;
import java.util.List;

public class BudgetAllocation {
    private String id;
    private String title;
    private double allocatedAmount;
    private double currentSpent;
    private int iconRes;
    private int themeColor;
    private List<BudgetAllocation> subAllocations;
    private List<Transaction> expenses;
    private boolean isExpanded = false;

    public BudgetAllocation(String id, String title, double allocatedAmount, double currentSpent, int iconRes, int themeColor) {
        this.id = id;
        this.title = title;
        this.allocatedAmount = allocatedAmount;
        this.currentSpent = currentSpent;
        this.iconRes = iconRes;
        this.themeColor = themeColor;
        this.subAllocations = new ArrayList<>();
        this.expenses = new ArrayList<>();
    }

    public void setExpenses(List<Transaction> expenses) { this.expenses = expenses; }
    public List<Transaction> getExpenses() { return expenses; }
    public boolean hasExpenses() { return expenses != null && !expenses.isEmpty(); }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public double getAllocatedAmount() { return allocatedAmount; }
    public double getCurrentSpent() { return currentSpent; }
    public void setCurrentSpent(double spent) { this.currentSpent = spent; }
    public int getIconRes() { return iconRes; }
    public int getThemeColor() { return themeColor; }
    public List<BudgetAllocation> getSubAllocations() { return subAllocations; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
    public void addSubAllocation(BudgetAllocation sub) { this.subAllocations.add(sub); }

    public boolean hasSubAllocations() {
        return subAllocations != null && !subAllocations.isEmpty();
    }

    public static final DiffUtil.ItemCallback<BudgetAllocation> DIFF_CALLBACK = new DiffUtil.ItemCallback<BudgetAllocation>() {
        @Override
        public boolean areItemsTheSame(@NonNull BudgetAllocation oldItem, @NonNull BudgetAllocation newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BudgetAllocation oldItem, @NonNull BudgetAllocation newItem) {
            return oldItem.isExpanded == newItem.isExpanded &&
                    oldItem.allocatedAmount == newItem.allocatedAmount &&
                    oldItem.currentSpent == newItem.currentSpent;
        }
    };
}
