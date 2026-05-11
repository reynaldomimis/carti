package com.upreyvan.carti.model;

public class AiSuggestion {
    private final String text;
    private final int iconResId;
    private final int iconColor;
    private final int bgColor;

    public AiSuggestion(String text, int iconResId, int iconColor, int bgColor) {
        this.text = text;
        this.iconResId = iconResId;
        this.iconColor = iconColor;
        this.bgColor = bgColor;
    }

    public String getText() {
        return text;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getIconColor() {
        return iconColor;
    }

    public int getBgColor() {
        return bgColor;
    }
}