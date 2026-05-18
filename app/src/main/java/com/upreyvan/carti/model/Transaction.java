package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey
    @NonNull
    private String id;
    private String familyId;
    private String title;
    private String description;
    private String timestamp;
    private double amount;
    private int iconRes;
    private int iconBgColor;
    private int iconColor;
    private long timestampMillis;
    private String type;

    @Ignore
    public Transaction() {
        this.id = "";
        this.familyId = "";
        this.title = "Title";
        this.description = "Description";
        this.timestamp = "";
        this.amount = 0.00;
        this.type = "EXPENSE";
    }

    public Transaction(@NonNull String id, String familyId, String title, String description, String timestamp, double amount, int iconRes, int iconBgColor, int iconColor, long timestampMillis, String type) {
        this.id = id;
        this.familyId = familyId;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.amount = amount;
        this.iconRes = iconRes;
        this.iconBgColor = iconBgColor;
        this.iconColor = iconColor;
        this.timestampMillis = timestampMillis;
        this.type = type;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    public int getIconBgColor() { return iconBgColor; }
    public void setIconBgColor(int iconBgColor) { this.iconBgColor = iconBgColor; }
    public int getIconColor() { return iconColor; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmountDouble() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Double.compare(that.amount, amount) == 0 &&
                iconRes == that.iconRes &&
                iconBgColor == that.iconBgColor &&
                iconColor == that.iconColor &&
                timestampMillis == that.timestampMillis &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, amount, iconRes, iconBgColor, iconColor, timestampMillis, type);
    }

    public static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.equals(newItem);
        }
    };
}
