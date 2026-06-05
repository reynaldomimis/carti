package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.upreyvan.carti.model.Comment;
import java.util.List;

@Dao
public interface CommentDao {
    @Query("SELECT * FROM comments WHERE transactionId = :transactionId ORDER BY createdAt ASC")
    LiveData<List<Comment>> getCommentsForTransaction(String transactionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Comment comment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Comment> comments);

    @Query("DELETE FROM comments WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM comments WHERE transactionId = :transactionId")
    void deleteByTransactionId(String transactionId);
}
