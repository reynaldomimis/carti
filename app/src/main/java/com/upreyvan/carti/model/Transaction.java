package com.upreyvan.carti.model;

import java.util.Objects;

public class Transaction {
    private String id;
    private final String title;
    private final String timestamp;
    private final String amount;
    private final int iconRes;
    private final int iconBgColor;
    private final int iconColor;
    private final long timestampMillis;
    private final String type;

    public Transaction(String id, String title, String timestamp, String amount, int iconRes, int iconBgColor) {
        this(id, title, timestamp, amount, iconRes, iconBgColor, 0, System.currentTimeMillis(), "EXPENSE");
    }

    public Transaction(String id, String title, String timestamp, String amount, int iconRes, int iconBgColor, int iconColor) {
        this(id, title, timestamp, amount, iconRes, iconBgColor, iconColor, System.currentTimeMillis(), "EXPENSE");
    }

    public Transaction(String id, String title, String timestamp, String amount, int iconRes, int iconBgColor, int iconColor, long timestampMillis) {
        this(id, title, timestamp, amount, iconRes, iconBgColor, iconColor, timestampMillis, "EXPENSE");
    }

    public Transaction(String id, String title, String timestamp, String amount, int iconRes, int iconBgColor, int iconColor, long timestampMillis, String type) {
        this.id = id;
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

    public String getId() { return id; }

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
