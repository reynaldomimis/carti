package com.upreyvan.carti.model;

import java.io.Serializable;

public class QuickLogItem implements Serializable {
    private String label;
    private int iconRes;
    private int bgColor;
    private int iconColor;

    public QuickLogItem(String label, int iconRes, int bgColor, int iconColor) {
        this.label = label;
        this.iconRes = iconRes;
        this.bgColor = bgColor;
        this.iconColor = iconColor;
    }

    public String getTitle() {
        return label;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getBgColor() {
        return bgColor;
    }

    public int getIconColor() {
        return iconColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuickLogItem that = (QuickLogItem) o;
        return iconRes == that.iconRes &&
                bgColor == that.bgColor &&
                iconColor == that.iconColor &&
                java.util.Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(label, iconRes, bgColor, iconColor);
    }

    public static final androidx.recyclerview.widget.DiffUtil.ItemCallback<QuickLogItem> DIFF_CALLBACK =
            new androidx.recyclerview.widget.DiffUtil.ItemCallback<QuickLogItem>() {
                @Override
                public boolean areItemsTheSame(@androidx.annotation.NonNull QuickLogItem oldItem, @androidx.annotation.NonNull QuickLogItem newItem) {
                    return oldItem.label.equals(newItem.label);
                }

                @Override
                public boolean areContentsTheSame(@androidx.annotation.NonNull QuickLogItem oldItem, @androidx.annotation.NonNull QuickLogItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
