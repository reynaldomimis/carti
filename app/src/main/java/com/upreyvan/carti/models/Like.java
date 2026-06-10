package com.upreyvan.carti.models;

import androidx.annotation.NonNull;
import java.util.Objects;

public class Like {
    @NonNull
    private String id;
    private String transactionId;
    private String userId;
    private String username;
    private String emojiType;
    private String updatedAt;

    public Like(@NonNull String id, String transactionId, String userId, String username, String emojiType) {
        this.id = id;
        this.transactionId = transactionId;
        this.userId = userId;
        this.username = username;
        this.emojiType = emojiType;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmojiType() { return emojiType; }
    public void setEmojiType(String emojiType) { this.emojiType = emojiType; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Like like = (Like) o;
        return Objects.equals(id, like.id) &&
                Objects.equals(transactionId, like.transactionId) &&
                Objects.equals(userId, like.userId) &&
                Objects.equals(username, like.username) &&
                Objects.equals(emojiType, like.emojiType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionId, userId, username, emojiType);
    }
}
