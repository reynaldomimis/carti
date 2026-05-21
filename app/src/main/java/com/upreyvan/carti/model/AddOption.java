package com.upreyvan.carti.model;

import androidx.fragment.app.Fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

public class AddOption {
    private final int iconResId;
    private final int iconTintResId;
    private final int bgTintResId;
    private final int titleResId;
    private final int descResId;
    private final Fragment fragment;

    public AddOption(int iconResId, int iconTintResId, int bgTintResId, int titleResId, int descResId, Fragment fragment) {
        this.iconResId = iconResId;
        this.iconTintResId = iconTintResId;
        this.bgTintResId = bgTintResId;
        this.titleResId = titleResId;
        this.descResId = descResId;
        this.fragment = fragment;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getIconTintResId() {
        return iconTintResId;
    }

    public int getBgTintResId() {
        return bgTintResId;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getDescResId() {
        return descResId;
    }

    public Fragment getFragment() {
        return fragment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddOption addOption = (AddOption) o;
        return iconResId == addOption.iconResId &&
                iconTintResId == addOption.iconTintResId &&
                bgTintResId == addOption.bgTintResId &&
                titleResId == addOption.titleResId &&
                descResId == addOption.descResId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(iconResId, iconTintResId, bgTintResId, titleResId, descResId);
    }

    public static final DiffUtil.ItemCallback<AddOption> DIFF_CALLBACK = new DiffUtil.ItemCallback<AddOption>() {
        @Override
        public boolean areItemsTheSame(@NonNull AddOption oldItem, @NonNull AddOption newItem) {
            return oldItem.titleResId == newItem.titleResId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull AddOption oldItem, @NonNull AddOption newItem) {
            return oldItem.equals(newItem);
        }
    };
}
