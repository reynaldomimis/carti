package com.upreyvan.carti.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.upreyvan.carti.model.Like;

import java.util.List;

@Dao
public interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Like like);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Like> likes);

    @Delete
    void delete(Like like);

    @Query("DELETE FROM likes WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM likes WHERE transactionId = :transactionId AND userId = :userId")
    void deleteUserLike(String transactionId, String userId);

    @Query("SELECT * FROM likes WHERE transactionId = :transactionId")
    List<Like> getLikesForTransaction(String transactionId);

    @Query("SELECT group_concat(username, ', ') FROM likes WHERE transactionId = :transactionId")
    String getReactorNames(String transactionId);

    @Query("SELECT l.username as username, l.emojiType as emoji, l.userId as userId, m.avatarRes as avatarRes, m.avatarUrl as avatarUrl " +
           "FROM likes l " +
           "LEFT JOIN members m ON l.userId = m.id " +
           "WHERE l.transactionId = :transactionId")
    androidx.lifecycle.LiveData<java.util.List<com.upreyvan.carti.model.Reactor>> getReactorsForTransaction(String transactionId);
}
