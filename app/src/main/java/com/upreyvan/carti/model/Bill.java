package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class Bill {
    private final String id;
    private final String familyId;
    private final String name;
    private final String date;
    private final String amount;
    private final String status;
    private final int iconResId;

    public Bill(String id, String familyId, String name, String date, String amount, String status, int iconResId) {
        this.id = id;
        this.familyId = familyId;
        this.name = name;
        this.date = date;
        this.amount = amount;
        this.status = status;
        this.iconResId = iconResId;
    }

    public String getId() { return id; }
    public String getFamilyId() { return familyId; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public String getAmount() { return amount; }
    public String getStatus() { return status; }
    public int getIconResId() { return iconResId; }

    public static final DiffUtil.ItemCallback<Bill> DIFF_CALLBACK = new DiffUtil.ItemCallback<Bill>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return Objects.equals(oldItem.id, newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return Objects.equals(oldItem.name, newItem.name) &&
                    Objects.equals(oldItem.date, newItem.date) &&
                    Objects.equals(oldItem.amount, newItem.amount) &&
                    Objects.equals(oldItem.status, newItem.status) &&
                    oldItem.iconResId == newItem.iconResId;
        }
    };
}
