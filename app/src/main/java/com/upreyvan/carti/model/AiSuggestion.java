package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

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

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
    public int getThemeColor() { return themeColor; }
    public String getActionText() { return actionText; }
    public String getType() { return type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiSuggestion that = (AiSuggestion) o;
        return iconResId == that.iconResId &&
                themeColor == that.themeColor &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(actionText, that.actionText) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, iconResId, themeColor, actionText, type);
    }

    public static final DiffUtil.ItemCallback<AiSuggestion> DIFF_CALLBACK = new DiffUtil.ItemCallback<AiSuggestion>() {
        @Override
        public boolean areItemsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
            return Objects.equals(oldItem.title, newItem.title);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AiSuggestion oldItem, @NonNull AiSuggestion newItem) {
            return oldItem.equals(newItem);
        }
    };
}
