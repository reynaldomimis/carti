package com.upreyvan.carti.model;

import androidx.fragment.app.Fragment;

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
}
