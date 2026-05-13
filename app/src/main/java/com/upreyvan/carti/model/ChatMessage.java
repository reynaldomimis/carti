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

    public ChatMessage(String senderName,
                       String message,
                       String time,
                       boolean isMe,
                       int imageResId) {
        this(senderName, message, time, isMe, imageResId, IntentType.UNKNOWN);
    }

    public ChatMessage(String senderName,
                       String message,
                       String time,
                       boolean isMe,
                       int imageResId,
                       IntentType intent) {
        this.senderName = senderName;
        this.message = message;
        this.time = time;
        this.isMe = isMe;
        this.imageResId = imageResId;
        this.intent = intent;
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

    public IntentType getIntent() {
        return intent;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

    public boolean isCancelable() {
        return intent == IntentType.EXPENSE_LOG || intent == IntentType.INCOME_LOG;
    }
}