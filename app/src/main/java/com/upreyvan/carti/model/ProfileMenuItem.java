package com.upreyvan.carti.model;

import androidx.fragment.app.Fragment;

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
}
