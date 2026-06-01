package com.upreyvan.carti.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.List;

@Dao
public interface TransactionDao {
    @Query("""
        SELECT transactions.*, members.title as memberUsername, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl,
        my_likes.emojiType as myReaction,
        (SELECT group_concat(username, ', ') FROM likes WHERE likes.transactionId = transactions.id) as reactorNames
        FROM transactions
        LEFT JOIN members ON transactions.userId = members.id
        LEFT JOIN likes AS my_likes ON transactions.id = my_likes.transactionId AND my_likes.userId = :currentUserId
        WHERE transactions.familyId = :familyId ORDER BY timestampMillis DESC
        """)
    LiveData<List<TransactionWithUser>> getAllTransactions(String familyId, String currentUserId);

    @Query("""
        SELECT transactions.*, members.title as memberUsername, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl,
        my_likes.emojiType as myReaction,
        (SELECT group_concat(username, ', ') FROM likes WHERE likes.transactionId = transactions.id) as reactorNames
        FROM transactions
        LEFT JOIN members ON transactions.userId = members.id
        LEFT JOIN likes AS my_likes ON transactions.id = my_likes.transactionId AND my_likes.userId = :currentUserId
        WHERE transactions.familyId = :familyId AND transactions.type = :type ORDER BY timestampMillis DESC
        """)
    LiveData<List<TransactionWithUser>> getTransactionsByType(String familyId, String type, String currentUserId);

    @Query("""
        SELECT transactions.*, members.title as memberUsername, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl,
        my_likes.emojiType as myReaction,
        (SELECT group_concat(username, ', ') FROM likes WHERE likes.transactionId = transactions.id) as reactorNames
        FROM transactions
        LEFT JOIN members ON transactions.userId = members.id
        LEFT JOIN likes AS my_likes ON transactions.id = my_likes.transactionId AND my_likes.userId = :currentUserId
        WHERE transactions.familyId = :familyId AND timestampMillis >= :start AND timestampMillis <= :end
        ORDER BY timestampMillis DESC
        """)
    LiveData<List<TransactionWithUser>> getTransactionsInRange(String familyId, long start, long end, String currentUserId);

    @Query("""
        SELECT transactions.*, members.title as memberUsername, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl,
        my_likes.emojiType as myReaction,
        (SELECT group_concat(username, ', ') FROM likes WHERE likes.transactionId = transactions.id) as reactorNames
        FROM transactions
        LEFT JOIN members ON transactions.userId = members.id
        LEFT JOIN likes AS my_likes ON transactions.id = my_likes.transactionId AND my_likes.userId = :currentUserId
        WHERE transactions.familyId = :familyId ORDER BY timestampMillis DESC LIMIT :limit
        """)
    LiveData<List<TransactionWithUser>> getRecentTransactions(String familyId, int limit, String currentUserId);

    @Query("""
        SELECT transactions.*, members.title as memberUsername, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl,
        my_likes.emojiType as myReaction,
        (SELECT group_concat(username, ', ') FROM likes WHERE likes.transactionId = transactions.id) as reactorNames
        FROM transactions
        LEFT JOIN members ON transactions.userId = members.id
        LEFT JOIN likes AS my_likes ON transactions.id = my_likes.transactionId AND my_likes.userId = :currentUserId
        WHERE transactions.id = :id
        """)
    LiveData<TransactionWithUser> getTransactionById(String id, String currentUserId);

    @Query("SELECT * FROM transactions WHERE id = :id")
    LiveData<Transaction> getTransactionByIdRaw(String id);

    @Query("SELECT * FROM transactions WHERE id = :id")
    Transaction getTransactionByIdRawSync(String id);

    @Query("""
        SELECT transactions.*, members.title as memberUsername, members.description as userRole, members.avatarRes as userAvatarRes, members.avatarUrl as userAvatarUrl,
        my_likes.emojiType as myReaction,
        (SELECT group_concat(username, ', ') FROM likes WHERE likes.transactionId = transactions.id) as reactorNames
        FROM transactions
        LEFT JOIN members ON transactions.userId = members.id
        LEFT JOIN likes AS my_likes ON transactions.id = my_likes.transactionId AND my_likes.userId = :currentUserId
        WHERE transactions.familyId = :familyId AND transactions.allocatedTo = :goalId ORDER BY timestampMillis DESC
        """)
    LiveData<List<TransactionWithUser>> getTransactionsByAllocation(String familyId, String goalId, String currentUserId);

    @Query("SELECT SUM(amount) FROM transactions WHERE familyId = :familyId AND timestampMillis >= :startOfDay AND type = 'EXPENSE'")
    LiveData<Double> getTodayTotalSpent(String familyId, long startOfDay);

    @Query("SELECT SUM(amount) FROM transactions WHERE familyId = :familyId AND type = 'INCOME'")
    LiveData<Double> getTotalIncome(String familyId);

    @Query("SELECT SUM(amount) FROM transactions WHERE familyId = :familyId AND type = 'INCOME' AND timestampMillis >= :start AND timestampMillis <= :end")
    LiveData<Double> getTotalIncomeInRange(String familyId, long start, long end);

    @Query("SELECT SUM(amount) FROM transactions WHERE familyId = :familyId AND type = 'EXPENSE' AND timestampMillis >= :start AND timestampMillis <= :end")
    LiveData<Double> getTotalExpenseInRange(String familyId, long start, long end);

    @Query("SELECT * FROM transactions WHERE familyId = :familyId")
    List<Transaction> getAllTransactionsList(String familyId);

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE familyId = :familyId AND type = 'EXPENSE' GROUP BY category ORDER BY total DESC")
    List<CategorySum> getExpenseBreakdown(String familyId);

    class CategorySum {
        public String category;
        public double total;
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Transaction transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Transaction> transactions);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    void deleteById(String transactionId);

    @Query("DELETE FROM transactions WHERE familyId = :familyId")
    void deleteAll(String familyId);
}
