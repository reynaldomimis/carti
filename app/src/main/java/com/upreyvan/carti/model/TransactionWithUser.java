package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Embedded;

import java.util.Objects;

public class TransactionWithUser {
    @Embedded
    private Transaction transaction;

    private String username;
    private String userRole;
    private int userAvatarRes;
    private String userAvatarUrl;

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public int getUserAvatarRes() {
        return userAvatarRes;
    }

    public void setUserAvatarRes(int userAvatarRes) {
        this.userAvatarRes = userAvatarRes;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionWithUser that = (TransactionWithUser) o;
        return Objects.equals(transaction, that.transaction) &&
                Objects.equals(username, that.username) &&
                Objects.equals(userRole, that.userRole) &&
                Objects.equals(userAvatarUrl, that.userAvatarUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction, username, userRole, userAvatarUrl);
    }

    public static final DiffUtil.ItemCallback<TransactionWithUser> DIFF_CALLBACK = new DiffUtil.ItemCallback<TransactionWithUser>() {
        @Override
        public boolean areItemsTheSame(@NonNull TransactionWithUser oldItem, @NonNull TransactionWithUser newItem) {
            return oldItem.getTransaction().getId().equals(newItem.getTransaction().getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull TransactionWithUser oldItem, @NonNull TransactionWithUser newItem) {
            return oldItem.equals(newItem);
        }
    };
}
