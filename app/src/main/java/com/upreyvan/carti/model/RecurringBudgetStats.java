package com.upreyvan.carti.model;

public class RecurringBudgetStats {
    private final int totalRecurringItems;
    private final double totalRecurringAmount;
    private final String lastProcessedMonth;
    private final boolean needsProcessing;

    public RecurringBudgetStats(int totalRecurringItems, double totalRecurringAmount, String lastProcessedMonth, boolean needsProcessing) {
        this.totalRecurringItems = totalRecurringItems;
        this.totalRecurringAmount = totalRecurringAmount;
        this.lastProcessedMonth = lastProcessedMonth;
        this.needsProcessing = needsProcessing;
    }

    public int getTotalRecurringItems() { return totalRecurringItems; }
    public double getTotalRecurringAmount() { return totalRecurringAmount; }
    public String getLastProcessedMonth() { return lastProcessedMonth; }
    public boolean isNeedsProcessing() { return needsProcessing; }
}
