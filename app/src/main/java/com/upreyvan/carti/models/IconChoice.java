package com.upreyvan.carti.models;

import androidx.annotation.DrawableRes;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

public class IconChoice {
    private String name;
    private @DrawableRes int iconRes;
    private boolean isSelected;

    public IconChoice(String name, int iconRes) {
        this(name, iconRes, false);
    }

    public IconChoice(String name, int iconRes, boolean isSelected) {
        this.name = name;
        this.iconRes = iconRes;
        this.isSelected = isSelected;
    }

    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IconChoice that = (IconChoice) o;
        return iconRes == that.iconRes && isSelected == that.isSelected && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, iconRes, isSelected);
    }

    public static final DiffUtil.ItemCallback<IconChoice> DIFF_CALLBACK = new DiffUtil.ItemCallback<IconChoice>() {
        @Override
        public boolean areItemsTheSame(@NonNull IconChoice oldItem, @NonNull IconChoice newItem) {
            return oldItem.name.equals(newItem.name);
        }

        @Override
        public boolean areContentsTheSame(@NonNull IconChoice oldItem, @NonNull IconChoice newItem) {
            return oldItem.equals(newItem);
        }
    };
}
