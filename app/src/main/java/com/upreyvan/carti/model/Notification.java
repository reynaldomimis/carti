package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class Notification {
    public enum Type {
        INFO,
        JOIN_REQUEST
    }

    private String title;
    private String description;
    private long timestamp;
    private Type type;
    private String applicantId;

    public Notification(String title, String description, long timestamp) {
        this(title, description, timestamp, Type.INFO, null);
    }

    public Notification(String title, String description, long timestamp, Type type, String applicantId) {
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.type = type;
        this.applicantId = applicantId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }
    public Type getType() { return type; }
    public String getApplicantId() { return applicantId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return timestamp == that.timestamp &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, timestamp, type);
    }

    public static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getTimestamp() == newItem.getTimestamp() && oldItem.getTitle().equals(newItem.getTitle());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.equals(newItem);
        }
    };
}
