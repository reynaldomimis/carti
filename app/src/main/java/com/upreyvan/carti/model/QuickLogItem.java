package com.upreyvan.carti.model;

import java.io.Serializable;

public class QuickLogItem implements Serializable {
    private String label;
    private int iconRes;
    private int bgColor;
    private int iconColor;
    private boolean isShimmer = false;

    public QuickLogItem(String label, int iconRes, int bgColor, int iconColor) {
        this.label = label;
        this.iconRes = iconRes;
        this.bgColor = bgColor;
        this.iconColor = iconColor;
    }

    public QuickLogItem(boolean isShimmer) {
        this.isShimmer = isShimmer;
    }

    public String getTitle() {
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

    public boolean isShimmer() {
        return isShimmer;
    }
}
