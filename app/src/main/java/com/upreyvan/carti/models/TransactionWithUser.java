package com.upreyvan.carti.models;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionWithUser {
    private Transaction transaction;

    private String memberUsername;
    private String userRole;
    private int userAvatarRes;
    private String userAvatarUrl;
    private String myReaction;
    private String myLikeId;

    private List<Like> reactions;

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void setMemberUsername(String memberUsername) {
        this.memberUsername = memberUsername;
    }

    public String getUsername() {
        return memberUsername != null ? memberUsername : (transaction != null ? transaction.getUsername() : null);
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

    public String getMyLikeId() {
        return myLikeId;
    }

    public void setMyLikeId(String myLikeId) {
        this.myLikeId = myLikeId;
    }

    public List<Like> getReactions() {
        return reactions;
    }

    public void setReactions(List<Like> reactions) {
        this.reactions = reactions;
    }

    public String getReactorNames() {
        if (reactions == null || reactions.isEmpty()) return "";

        java.util.Map<String, String> uniqueReactors = new java.util.LinkedHashMap<>();
        for (Like l : reactions) {
            if (l.getUserId() != null && l.getUsername() != null) {
                uniqueReactors.put(l.getUserId(), l.getUsername());
            }
        }

        StringBuilder sb = new StringBuilder();
        java.util.List<String> names = new java.util.ArrayList<>(uniqueReactors.values());
        for (int i = 0; i < names.size(); i++) {
            sb.append(names.get(i));
            if (i < names.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public TransactionWithUser copy() {
        TransactionWithUser copy = new TransactionWithUser();
        copy.transaction = this.transaction != null ? this.transaction.copy() : null;
        copy.memberUsername = this.memberUsername;
        copy.userRole = this.userRole;
        copy.userAvatarRes = this.userAvatarRes;
        copy.userAvatarUrl = this.userAvatarUrl;
        copy.myReaction = this.myReaction;
        copy.myLikeId = this.myLikeId;
        copy.reactions = this.reactions != null ? new ArrayList<>(this.reactions) : null;
        return copy;
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
                Objects.equals(reactions, that.reactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction, memberUsername, userRole, userAvatarUrl, myReaction, reactions);
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
