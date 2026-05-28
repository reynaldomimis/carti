package com.upreyvan.carti.model;

import androidx.fragment.app.Fragment;

public class ProfileMenuItem {
    private final int iconResId;
    private final int titleResId;
    private final String subTitle;
    private final Class<? extends Fragment> fragmentClass;
    private final boolean showDivider;

    public ProfileMenuItem(int iconResId, int titleResId, String subTitle, Class<? extends Fragment> fragmentClass) {
        this(iconResId, titleResId, subTitle, fragmentClass, true);
    }

    public ProfileMenuItem(int iconResId, int titleResId, String subTitle, Class<? extends Fragment> fragmentClass, boolean showDivider) {
        this.iconResId = iconResId;
        this.titleResId = titleResId;
        this.subTitle = subTitle;
        this.fragmentClass = fragmentClass;
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

    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }

    public boolean isShowDivider() {
        return showDivider;
    }

    public static final androidx.recyclerview.widget.DiffUtil.ItemCallback<ProfileMenuItem> DIFF_CALLBACK = new androidx.recyclerview.widget.DiffUtil.ItemCallback<ProfileMenuItem>() {
        @Override
        public boolean areItemsTheSame(@androidx.annotation.NonNull ProfileMenuItem oldItem, @androidx.annotation.NonNull ProfileMenuItem newItem) {
            return oldItem.titleResId == newItem.titleResId;
        }

        @Override
        public boolean areContentsTheSame(@androidx.annotation.NonNull ProfileMenuItem oldItem, @androidx.annotation.NonNull ProfileMenuItem newItem) {
            return oldItem.subTitle.equals(newItem.subTitle);
        }
    };
}
