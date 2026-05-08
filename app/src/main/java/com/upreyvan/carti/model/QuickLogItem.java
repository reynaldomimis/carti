package com.upreyvan.carti.model;

public class QuickLogItem {
    private String label;
    private int iconRes;
    private int bgColor;
    private int iconColor;

    public QuickLogItem(String label, int iconRes, int bgColor, int iconColor) {
        this.label = label;
        this.iconRes = iconRes;
        this.bgColor = bgColor;
        this.iconColor = iconColor;
    }

    public String getLabel() {
        return label;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getBgColor() {
        return bgColor;
    }

    public int getIconColor() {
        return iconColor;
    }
}
