package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class Comment {
    @NonNull
    private String id;
    private String transactionId;
    private String userId;
    private String username;
    private String text;
    private String parentId;
    private String createdAt;
    private String updatedAt;

    public Comment(@NonNull String id, String transactionId, String userId, String username, String text, String parentId, String createdAt, String updatedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getText() { return text; }
    public String getParentId() { return parentId; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(id, comment.id) &&
                Objects.equals(text, comment.text) &&
                Objects.equals(parentId, comment.parentId) &&
                Objects.equals(updatedAt, comment.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, parentId, updatedAt);
    }

    public static final DiffUtil.ItemCallback<Comment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Comment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.equals(newItem);
        }
    };
}
