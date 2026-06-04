package com.upreyvan.carti.data.local.source;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.List;

public class TransactionLocalDataSource {
    private final TransactionDao transactionDao;

    public TransactionLocalDataSource(Context context) {
        this.transactionDao = AppDatabase.getInstance(context).transactionDao();
    }

    public LiveData<List<TransactionWithUser>> getAllTransactions(String familyId, String currentUserId) {
        return transactionDao.getAllTransactions(familyId, currentUserId);
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByType(String familyId, String type, String currentUserId) {
        return transactionDao.getTransactionsByType(familyId, type, currentUserId);
    }

    public LiveData<List<TransactionWithUser>> getRecentTransactions(String familyId, String currentUserId, int limit) {
        return transactionDao.getRecentTransactions(familyId, limit, currentUserId);
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByAllocation(String familyId, String goalId, String currentUserId) {
        return transactionDao.getTransactionsByAllocation(familyId, goalId, currentUserId);
    }

    public LiveData<List<TransactionWithUser>> getTransactionsInRange(String familyId, long start, long end, String currentUserId) {
        return transactionDao.getTransactionsInRange(familyId, start, end, currentUserId);
    }

    public LiveData<TransactionWithUser> getTransactionById(String id, String currentUserId) {
        return transactionDao.getTransactionById(id, currentUserId);
    }

    public List<Transaction> getAllTransactionsList(String familyId) {
        return transactionDao.getAllTransactionsList(familyId);
    }

    public void saveTransactions(List<Transaction> transactions) {
        transactionDao.insertAll(transactions);
    }

    public void deleteTransactionById(String id) {
        transactionDao.deleteById(id);
    }

    public void clearCache(String familyId) {
        transactionDao.deleteAll(familyId);
    }
}
