package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT transactions.*, members.title as userName, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl " +
           "FROM transactions " +
           "LEFT JOIN members ON transactions.userId = members.id " +
           "WHERE transactions.familyId = :familyId ORDER BY timestampMillis DESC")
    LiveData<List<TransactionWithUser>> getAllTransactions(String familyId);

    @Query("SELECT transactions.*, members.title as userName, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl " +
           "FROM transactions " +
           "LEFT JOIN members ON transactions.userId = members.id " +
           "WHERE transactions.familyId = :familyId AND timestampMillis >= :start AND timestampMillis <= :end")
    LiveData<List<TransactionWithUser>> getTransactionsInRange(String familyId, long start, long end);

    @Query("SELECT transactions.*, members.title as userName, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl " +
           "FROM transactions " +
           "LEFT JOIN members ON transactions.userId = members.id " +
           "WHERE transactions.familyId = :familyId ORDER BY timestampMillis DESC LIMIT :limit")
    LiveData<List<TransactionWithUser>> getRecentTransactions(String familyId, int limit);

    @Query("SELECT * FROM transactions WHERE familyId = :familyId")
    List<Transaction> getAllTransactionsList(String familyId);

    @Query("SELECT SUM(CAST(REPLACE(REPLACE(amount, '₱', ''), ',', '') AS DOUBLE)) FROM transactions WHERE familyId = :familyId AND timestampMillis >= :startOfDay")
    LiveData<Double> getTodayTotalSpent(String familyId, long startOfDay);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Transaction> transactions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Transaction transaction);

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    void deleteById(String transactionId);

    @Query("DELETE FROM transactions WHERE familyId = :familyId")
    void deleteAll(String familyId);
}