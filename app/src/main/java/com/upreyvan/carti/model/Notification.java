package com.upreyvan.carti.model;

public class Notification {
    public enum Type {
        INFO,
        JOIN_REQUEST
    }

    private String title;
    private String description;
    private long timestamp;
    private Type type;

    public Notification(String title, String description, long timestamp) {
        this(title, description, timestamp, Type.INFO);
    }

    public Notification(String title, String description, long timestamp, Type type) {
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }
    public Type getType() { return type; }
}
