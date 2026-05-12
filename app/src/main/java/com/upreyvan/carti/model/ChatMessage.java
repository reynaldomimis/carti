package com.upreyvan.carti.model;

public class ChatMessage {

    private final String senderName;
    private final String message;
    private final String time;
    private final boolean isMe;

    private final int imageResId;

    public ChatMessage(String senderName,
                       String message,
                       String time,
                       boolean isMe,
                       int imageResId) {

        this.senderName = senderName;
        this.message = message;
        this.time = time;
        this.isMe = isMe;
        this.imageResId = imageResId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public boolean isMe() {
        return isMe;
    }

    public int getImageResId() {
        return imageResId;
    }
}