package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class Bill {
    private final String id;
    private final String familyId;
    private final String name;
    private final String date;
    private final String status;
    private final int iconResId;
    private final double amount;
    private final String category;

    public Bill(String id, String familyId, String name, String date, String status, int iconResId) {
        this(id, familyId, name, date, status, iconResId, 0.0, "Bill");
    }

    public Bill(String id, String familyId, String name, String date, String status, int iconResId, double amount, String category) {
        this.id = id;
        this.familyId = familyId;
        this.name = name;
        this.date = date;
        this.status = status;
        this.iconResId = iconResId;
        this.amount = amount;
        this.category = category;
    }

    public String getId() { return id; }
    public String getFamilyId() { return familyId; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public int getIconResId() { return iconResId; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }

    public static final DiffUtil.ItemCallback<Bill> DIFF_CALLBACK = new DiffUtil.ItemCallback<Bill>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return Objects.equals(oldItem.id, newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return Objects.equals(oldItem.name, newItem.name) &&
                    Objects.equals(oldItem.date, newItem.date) &&
                    Objects.equals(oldItem.status, newItem.status) &&
                    oldItem.iconResId == newItem.iconResId &&
                    oldItem.amount == newItem.amount &&
                    Objects.equals(oldItem.category, newItem.category);
        }
    };
}
