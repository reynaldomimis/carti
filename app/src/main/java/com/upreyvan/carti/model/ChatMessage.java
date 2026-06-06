package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.upreyvan.carti.data.ai.IntentType;

import org.json.JSONObject;

import java.util.Objects;

public class ChatMessage {

    private String senderName;
    private String message;
    private String time;
    private boolean isMe;
    private int imageResId;
    private IntentType intent;
    private boolean isCanceled = false;
    private boolean isShimmer = false;
    private boolean isSummary = false;
    private JSONObject pendingAction;
    private JSONObject summaryData;

    public boolean isSummary() { return isSummary; }
    public void setSummary(boolean summary) { isSummary = summary; }
    public JSONObject getSummaryData() { return summaryData; }
    public void setSummaryData(JSONObject summaryData) { this.summaryData = summaryData; }

    @NonNull
    private String id;
    private String senderId;
    private String familyId;
    private long timestamp;

    public ChatMessage(boolean isShimmer, boolean isMe) {
        this.id = "shimmer_" + System.currentTimeMillis();
        this.senderName = "";
        this.message = "";
        this.time = "";
        this.isMe = isMe;
        this.imageResId = 0;
        this.intent = IntentType.UNKNOWN;
        this.isShimmer = isShimmer;
    }

    public ChatMessage(String senderName, String message, String time, boolean isMe, int imageResId) {
        this.id = "msg_" + System.currentTimeMillis();
        this.senderName = senderName;
        this.message = message;
        this.time = time;
        this.isMe = isMe;
        this.imageResId = imageResId;
        this.intent = IntentType.UNKNOWN;
    }

    public ChatMessage(String senderName, String message, String time, boolean isMe, int imageResId, IntentType intent) {
        this(senderName, message, time, isMe, imageResId);
        this.intent = intent;
    }

    public ChatMessage(@NonNull String id, String senderId, String familyId, String senderName, String message, long timestamp, boolean isMe) {
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
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public String getFamilyId() { return familyId; }
    public long getTimestamp() { return timestamp; }

    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public IntentType getIntent() {
        return intent;
    }

    public void setIntent(IntentType intent) {
        this.intent = intent;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

    public boolean isShimmer() {
        return isShimmer;
    }

    public void setShimmer(boolean shimmer) {
        isShimmer = shimmer;
    }

    public org.json.JSONObject getPendingAction() {
        return pendingAction;
    }

    public void setPendingAction(org.json.JSONObject pendingAction) {
        this.pendingAction = pendingAction;
    }

    public boolean isCancelable() {
        return intent == IntentType.EXPENSE_LOG || intent == IntentType.INCOME_LOG 
                || intent == IntentType.GOAL_LOG || intent == IntentType.DEBT_LOG;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return timestamp == that.timestamp &&
                isMe == that.isMe &&
                isCanceled == that.isCanceled &&
                Objects.equals(id, that.id) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message, timestamp, isMe, isCanceled);
    }

    public static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            if (oldItem.isShimmer() && newItem.isShimmer()) return true;
            if (oldItem.isShimmer() || newItem.isShimmer()) return false;

            if (oldItem.id != null && newItem.id != null) {
                return oldItem.id.equals(newItem.id);
            }

            return oldItem.message.equals(newItem.message)
                    && Math.abs(oldItem.timestamp - newItem.timestamp) < 30000;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return Objects.equals(oldItem.id, newItem.id)
                    && oldItem.message.equals(newItem.message)
                    && oldItem.isCanceled == newItem.isCanceled;
        }
    };
}
