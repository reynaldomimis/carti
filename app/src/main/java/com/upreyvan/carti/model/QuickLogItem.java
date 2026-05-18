package com.upreyvan.carti.model;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.io.Serializable;
import java.util.Objects;

public class QuickLogItem implements Serializable {
    private String title;
    private int iconRes;
    private int bgColor;
    private int iconColor;

    public QuickLogItem(String title, int iconRes, int bgColor, int iconColor) {
        this.title = title;
        this.iconRes = iconRes;
        this.bgColor = bgColor;
        this.iconColor = iconColor;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    public int getBgColor() { return bgColor; }
    public void setBgColor(int bgColor) { this.bgColor = bgColor; }
    public int getIconColor() { return iconColor; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuickLogItem that = (QuickLogItem) o;
        return iconRes == that.iconRes &&
                bgColor == that.bgColor &&
                iconColor == that.iconColor &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, iconRes, bgColor, iconColor);
    }

    public static final DiffUtil.ItemCallback<QuickLogItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<QuickLogItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull QuickLogItem oldItem, @NonNull QuickLogItem newItem) {
            return oldItem.title.equals(newItem.title);
        }

        @Override
        public boolean areContentsTheSame(@NonNull QuickLogItem oldItem, @NonNull QuickLogItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
