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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpenseCategory that = (ExpenseCategory) o;
        return Double.compare(that.amount, amount) == 0 &&
                Float.compare(that.percentage, percentage) == 0 &&
                color == that.color &&
                java.util.Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, amount, percentage, color);
    }

    public static final androidx.recyclerview.widget.DiffUtil.ItemCallback<ExpenseCategory> DIFF_CALLBACK =
            new androidx.recyclerview.widget.DiffUtil.ItemCallback<ExpenseCategory>() {
                @Override
                public boolean areItemsTheSame(@androidx.annotation.NonNull ExpenseCategory oldItem, @androidx.annotation.NonNull ExpenseCategory newItem) {
                    return oldItem.name.equals(newItem.name);
                }

                @Override
                public boolean areContentsTheSame(@androidx.annotation.NonNull ExpenseCategory oldItem, @androidx.annotation.NonNull ExpenseCategory newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
