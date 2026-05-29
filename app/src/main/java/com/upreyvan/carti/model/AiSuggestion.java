package com.upreyvan.carti.model;

public class AiSuggestion {
    private final String title;
    private final String description;
    private final int iconResId;
    private final int themeColor;
    private final String actionText;
    private final String type;

    public AiSuggestion(String title, String description, int iconResId, int themeColor) {
        this(title, description, iconResId, themeColor, "", "generic");
    }

    public AiSuggestion(String title, String description, int iconResId, int themeColor, String actionText, String type) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.themeColor = themeColor;
        this.actionText = actionText;
        this.type = type;
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

    public String getType() {
        return type;
    }
}
