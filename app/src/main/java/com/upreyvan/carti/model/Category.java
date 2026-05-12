package com.upreyvan.carti.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

public class Category {
    private String id;
    private String name;
    private @DrawableRes int iconRes;
    private @ColorRes int iconColor;
    private @ColorRes int backgroundColor;
    private boolean isDefault;

    public Category(String id, String name, int iconRes, int iconColor, int backgroundColor, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
        this.iconColor = iconColor;
        this.backgroundColor = backgroundColor;
        this.isDefault = isDefault;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
    public int getIconColor() { return iconColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public boolean isDefault() { return isDefault; }
}
