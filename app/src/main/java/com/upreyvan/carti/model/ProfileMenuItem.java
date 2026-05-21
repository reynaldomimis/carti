package com.upreyvan.carti.model;

import androidx.fragment.app.Fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

public class ProfileMenuItem {
    private final int iconResId;
    private final int titleResId;
    private final String subTitle;
    private final Fragment fragment;
    private final boolean showDivider;

    public ProfileMenuItem(int iconResId, int titleResId, String subTitle, Fragment fragment) {
        this(iconResId, titleResId, subTitle, fragment, true);
    }

    public ProfileMenuItem(int iconResId, int titleResId, String subTitle, Fragment fragment, boolean showDivider) {
        this.iconResId = iconResId;
        this.titleResId = titleResId;
        this.subTitle = subTitle;
        this.fragment = fragment;
        this.showDivider = showDivider;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public boolean isShowDivider() {
        return showDivider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileMenuItem that = (ProfileMenuItem) o;
        return iconResId == that.iconResId &&
                titleResId == that.titleResId &&
                showDivider == that.showDivider &&
                Objects.equals(subTitle, that.subTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iconResId, titleResId, subTitle, showDivider);
    }

    public static final DiffUtil.ItemCallback<ProfileMenuItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProfileMenuItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProfileMenuItem oldItem, @NonNull ProfileMenuItem newItem) {
            return oldItem.titleResId == newItem.titleResId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProfileMenuItem oldItem, @NonNull ProfileMenuItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
