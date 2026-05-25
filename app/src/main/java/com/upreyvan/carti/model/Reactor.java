package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

public class Reactor {
    private String username;
    private String emoji;
    private int avatarRes;
    private String avatarUrl;
    private String userId;

    public Reactor(String username, String emoji, int avatarRes, String avatarUrl, String userId) {
        this.username = username;
        this.emoji = emoji;
        this.avatarRes = avatarRes;
        this.avatarUrl = avatarUrl;
        this.userId = userId;
    }

    public String getUsername() { return username; }
    public String getEmoji() { return emoji; }
    public int getAvatarRes() { return avatarRes; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reactor reactor = (Reactor) o;
        return avatarRes == reactor.avatarRes &&
                Objects.equals(username, reactor.username) &&
                Objects.equals(emoji, reactor.emoji) &&
                Objects.equals(avatarUrl, reactor.avatarUrl) &&
                Objects.equals(userId, reactor.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, emoji, avatarRes, avatarUrl, userId);
    }

    public static final DiffUtil.ItemCallback<Reactor> DIFF_CALLBACK = new DiffUtil.ItemCallback<Reactor>() {
        @Override
        public boolean areItemsTheSame(@NonNull Reactor oldItem, @NonNull Reactor newItem) {
            return Objects.equals(oldItem.userId, newItem.userId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Reactor oldItem, @NonNull Reactor newItem) {
            return oldItem.equals(newItem);
        }
    };
}
