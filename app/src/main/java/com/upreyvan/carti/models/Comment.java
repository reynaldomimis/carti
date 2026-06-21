package com.upreyvan.carti.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class Comment {
    @NonNull
    private final String id;
    @NonNull
    private final String transactionId;
    @NonNull
    private final String userId;
    @NonNull
    private final String username;
    @NonNull
    private final String text;
    @Nullable
    private final String parentId;
    @NonNull
    private final String createdAt;
    @NonNull
    private final String updatedAt;
    @Nullable
    private String childSignature;
    @NonNull
    private Status status = Status.SENT;

    public Comment(@NonNull String id, @NonNull String transactionId, @NonNull String userId, @NonNull String username, @NonNull String text, @Nullable String parentId, @NonNull String createdAt, @NonNull String updatedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getId() { return id; }
    @NonNull
    public String getTransactionId() { return transactionId; }
    @NonNull
    public String getUserId() { return userId; }
    @NonNull
    public String getUsername() { return username; }
    @NonNull
    public String getText() { return text; }
    @Nullable
    public String getParentId() { return parentId; }
    @NonNull
    public String getCreatedAt() { return createdAt; }
    @NonNull
    public String getUpdatedAt() { return updatedAt; }
    @Nullable
    public String getChildSignature() { return childSignature; }
    public void setChildSignature(@Nullable String childSignature) { this.childSignature = childSignature; }
    @NonNull
    public Status getStatus() { return status; }
    public void setStatus(@NonNull Status status) { this.status = status; }

    public enum Status {
        SENDING,
        SENT,
        FAILED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(getId(), comment.getId()) &&
                Objects.equals(getTransactionId(), comment.getTransactionId()) &&
                Objects.equals(getUserId(), comment.getUserId()) &&
                Objects.equals(getText(), comment.getText()) &&
                Objects.equals(getUsername(), comment.getUsername()) &&
                Objects.equals(getParentId(), comment.getParentId()) &&
                Objects.equals(getCreatedAt(), comment.getCreatedAt()) &&
                Objects.equals(getUpdatedAt(), comment.getUpdatedAt()) &&
                Objects.equals(getChildSignature(), comment.getChildSignature()) &&
                status == comment.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getTransactionId(), getUserId(), getText(), getUsername(), getParentId(), getCreatedAt(), getUpdatedAt(), getChildSignature(), status);
    }

    public static final DiffUtil.ItemCallback<Comment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Comment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.equals(newItem);
        }
    };
}
