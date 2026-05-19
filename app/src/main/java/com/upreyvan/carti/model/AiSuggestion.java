package com.upreyvan.carti.model;

public class AiSuggestion {
    private final String title;
    private final String description;
    private final int iconResId;
    private final int themeColor;
    private final String actionText;

    public AiSuggestion(String title, String description, int iconResId, int themeColor, String actionText) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.themeColor = themeColor;
        this.actionText = actionText;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getThemeColor() {
        return themeColor;
    }

    public String getActionText() {
        return actionText;
    }
}
