package com.upreyvan.carti.model;

import com.upreyvan.carti.data.ai.IntentType;

public class ChatMessage {

    private final String senderName;
    private final String message;
    private final String time;
    private final boolean isMe;
    private final int imageResId;
    private final IntentType intent;
    private boolean isCanceled = false;
    private boolean isShimmer = false;
    
    // Appwrite Fields
    private String id;
    private String senderId;
    private String familyId;
    private long timestamp;

    public ChatMessage(boolean isShimmer, boolean isMe) {
        this.senderName = "";
        this.message = "";
        this.time = "";
        this.isMe = isMe;
        this.imageResId = 0;
        this.intent = IntentType.UNKNOWN;
        this.isShimmer = isShimmer;
    }

    public ChatMessage(String senderName, String message, String time, boolean isMe, int imageResId) {
        this.senderName = senderName;
        this.message = message;
        this.time = time;
        this.isMe = isMe;
        this.imageResId = imageResId;
        this.intent = IntentType.UNKNOWN;
    }

    public ChatMessage(String senderName, String message, String time, boolean isMe, int imageResId, IntentType intent) {
        this.senderName = senderName;
        this.message = message;
        this.time = time;
        this.isMe = isMe;
        this.imageResId = imageResId;
        this.intent = intent;
    }

    public ChatMessage(String id, String senderId, String familyId, String senderName, String message, long timestamp, boolean isMe) {
        this.id = id;
        this.senderId = senderId;
        this.familyId = familyId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.isMe = isMe;
        this.time = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(timestamp));
        this.imageResId = 0;
        this.intent = IntentType.UNKNOWN;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getFamilyId() { return familyId; }
    public long getTimestamp() { return timestamp; }

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

    public IntentType getIntent() {
        return intent;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isShimmer() {
        return isShimmer;
    }

    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

    public boolean isCancelable() {
        return intent == IntentType.EXPENSE_LOG || intent == IntentType.INCOME_LOG;
    }
}