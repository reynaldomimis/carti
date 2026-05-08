package com.upreyvan.carti.model;

import java.util.Objects;

public class Transaction {
    private final String title;
    private final String timestamp;
    private final String amount;
    private final int iconRes;
    private final int iconBgColor;

    public Transaction(String title, String timestamp, String amount, int iconRes, int iconBgColor) {
        this.title = title;
        this.timestamp = timestamp;
        this.amount = amount;
        this.iconRes = iconRes;
        this.iconBgColor = iconBgColor;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return iconRes == that.iconRes &&
                iconBgColor == that.iconBgColor &&
                Objects.equals(title, that.title) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, timestamp, amount, iconRes, iconBgColor);
    }
}
