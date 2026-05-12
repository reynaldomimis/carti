package com.upreyvan.carti.model;

import androidx.annotation.DrawableRes;

public class IconChoice {
    private String name;
    private @DrawableRes int iconRes;

    public IconChoice(String name, int iconRes) {
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
}
