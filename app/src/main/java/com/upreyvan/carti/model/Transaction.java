package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
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
    private String timestamp;
    private String amount;
    private int iconRes;
    private int iconBgColor;
    private int iconColor;
    private long timestampMillis;
    private String type;

    @Ignore
    public Transaction() {
        this.id = "";
        this.familyId = "";
        this.title = "";
        this.timestamp = "";
        this.amount = "";
        this.type = "EXPENSE";
    }

    public Transaction(@NonNull String id, String familyId, String title, String timestamp, String amount, int iconRes, int iconBgColor, int iconColor, long timestampMillis, String type) {
        this.id = id;
        this.familyId = familyId;
        this.title = title;
        this.timestamp = timestamp;
        this.amount = amount;
        this.iconRes = iconRes;
        this.iconBgColor = iconBgColor;
        this.iconColor = iconColor;
        this.timestampMillis = timestampMillis;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public double getAmountDouble() {
        try {
            return Double.parseDouble(amount.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    @NonNull
    public String getId() { return id; }

    public String getFamilyId() { return familyId; }

    public void setId(@NonNull String id) { this.id = id; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public void setTitle(String title) { this.title = title; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    public void setIconBgColor(int iconBgColor) { this.iconBgColor = iconBgColor; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
    public void setType(String type) { this.type = type; }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getTitle() {
        return title;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAmount() {
        return amount;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getIconBgColor() {
        return iconBgColor;
    }

    public int getIconColor() {
        return iconColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return iconRes == that.iconRes &&
                iconBgColor == that.iconBgColor &&
                iconColor == that.iconColor &&
                Objects.equals(title, that.title) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, timestamp, amount, iconRes, iconBgColor, iconColor);
    }
}
