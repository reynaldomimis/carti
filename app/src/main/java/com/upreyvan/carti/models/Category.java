package com.upreyvan.carti.models;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

public class Category {
    private String id;
    private String name;
    private int iconRes;
    private int iconColor;
    private int backgroundColor;
    private boolean isDefault;
    private String parentCategory;

    public Category(String id, String name, int iconRes, int iconColor, int backgroundColor, boolean isDefault) {
        this(id, name, iconRes, iconColor, backgroundColor, isDefault, null);
    }

    public Category(String id, String name, int iconRes, int iconColor, int backgroundColor, boolean isDefault, String parentCategory) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
        this.iconColor = iconColor;
        this.backgroundColor = backgroundColor;
        this.isDefault = isDefault;
        this.parentCategory = parentCategory;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
    public int getIconColor() { return iconColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public boolean isDefault() { return isDefault; }
    public String getParentCategory() { return parentCategory; }

    public void setName(String name) { this.name = name; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    public void setIconColor(int iconColor) { this.iconColor = iconColor; }
    public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }
    public void setParentCategory(String parentCategory) { this.parentCategory = parentCategory; }
}
