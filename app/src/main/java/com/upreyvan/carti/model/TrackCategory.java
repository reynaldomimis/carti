package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class TrackCategory {
    private String name;
    private double amount;
    private float percentage;
    private int color;

    public TrackCategory(String name, double amount, float percentage, int color) {
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
        TrackCategory that = (TrackCategory) o;
        return Double.compare(that.amount, amount) == 0 &&
                Float.compare(that.percentage, percentage) == 0 &&
                color == that.color &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, amount, percentage, color);
    }

    public static final DiffUtil.ItemCallback<TrackCategory> DIFF_CALLBACK = new DiffUtil.ItemCallback<TrackCategory>() {
        @Override
        public boolean areItemsTheSame(@NonNull TrackCategory oldItem, @NonNull TrackCategory newItem) {
            return oldItem.name.equals(newItem.name);
        }

        @Override
        public boolean areContentsTheSame(@NonNull TrackCategory oldItem, @NonNull TrackCategory newItem) {
            return oldItem.equals(newItem);
        }
    };
}
