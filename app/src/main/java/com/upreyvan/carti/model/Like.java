package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "likes", indices = {@Index("transactionId")})
public class Like {
    @PrimaryKey
    @NonNull
    private String id;
    private String transactionId;
    private String userId;
    private String username;
    private String emojiType;

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
}
