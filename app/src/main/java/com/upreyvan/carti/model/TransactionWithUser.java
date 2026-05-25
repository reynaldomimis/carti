package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Embedded;

import java.util.Objects;

public class TransactionWithUser {
    @Embedded
    private Transaction transaction;

    private String memberUsername;
    private String userRole;
    private int userAvatarRes;
    private String userAvatarUrl;
    private String myReaction;
    private String reactorNames;

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getMemberUsername() {
        return memberUsername;
    }

    public void setMemberUsername(String memberUsername) {
        this.memberUsername = memberUsername;
    }

    public String getUsername() {
        return memberUsername != null ? memberUsername : (transaction != null ? transaction.getUsername() : null);
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

    public String getMyReaction() {
        return myReaction;
    }

    public void setMyReaction(String myReaction) {
        this.myReaction = myReaction;
    }

    public String getReactorNames() {
        return reactorNames;
    }

    public void setReactorNames(String reactorNames) {
        this.reactorNames = reactorNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionWithUser that = (TransactionWithUser) o;
        return Objects.equals(transaction, that.transaction) &&
                Objects.equals(memberUsername, that.memberUsername) &&
                Objects.equals(userRole, that.userRole) &&
                Objects.equals(userAvatarUrl, that.userAvatarUrl) &&
                Objects.equals(myReaction, that.myReaction) &&
                Objects.equals(reactorNames, that.reactorNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction, memberUsername, userRole, userAvatarUrl, myReaction, reactorNames);
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
