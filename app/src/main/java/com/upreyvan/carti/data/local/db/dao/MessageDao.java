package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.upreyvan.carti.model.ChatMessage;
import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE familyId = :familyId ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessages(String familyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatMessage message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChatMessage> messages);

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM messages WHERE familyId = :familyId")
    void deleteAll(String familyId);

    @Query("SELECT * FROM messages WHERE familyId = :familyId ORDER BY timestamp DESC LIMIT 1")
    ChatMessage getLastMessage(String familyId);
}
