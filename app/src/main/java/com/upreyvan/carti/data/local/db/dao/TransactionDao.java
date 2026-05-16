package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.upreyvan.carti.model.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE familyId = :familyId ORDER BY timestampMillis DESC")
    LiveData<List<Transaction>> getAllTransactions(String familyId);

    @Query("SELECT * FROM transactions WHERE familyId = :familyId AND timestampMillis >= :start AND timestampMillis <= :end")
    LiveData<List<Transaction>> getTransactionsInRange(String familyId, long start, long end);

    @Query("SELECT * FROM transactions WHERE familyId = :familyId ORDER BY timestampMillis DESC LIMIT :limit")
    LiveData<List<Transaction>> getRecentTransactions(String familyId, int limit);

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