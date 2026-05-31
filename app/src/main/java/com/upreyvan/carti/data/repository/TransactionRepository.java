package com.upreyvan.carti.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.core.content.ContextCompat;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.model.Like;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private static TransactionRepository instance;
    private final TransactionDao transactionDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TransactionRepository(Context context) {
        Context appContext = context.getApplicationContext();
        transactionDao = AppDatabase.getInstance(appContext).transactionDao();
        apiHelper = new ApiHelper(appContext);
        pref = new PreferenceManager(appContext);
    }

    public static synchronized TransactionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionRepository(context);
        }
        return instance;
    }

    public void syncTransactionsIfNeeded() {
        syncCurrentMonth();
        syncIncomes();
        syncGoals();
        syncDebts();
        syncLikes();
    }

    public void syncLikes() {
        apiHelper.listAllLikes(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
                    List<Like> likes = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        String username = (String) data.get("username");
                        if (username == null) username = (String) data.get("userName");

                        likes.add(new Like(
                            doc.getId(),
                            String.valueOf(data.get("transactionId")),
                            String.valueOf(data.get("userId")),
                            username,
                            String.valueOf(data.get("emojiType"))
                        ));
                    }
                    AppDatabase.getInstance(apiHelper.getContext()).likeDao().insertAll(likes);
                });
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "Likes sync failed: " + error.getMessage());
            }
        });
    }

    public void refreshTransactions() {
        syncTransactionsIfNeeded();
    }

    public void syncCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        String start = Utils.getMonthStart(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR));
        String end = Utils.getMonthEnd(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR));

        apiHelper.getTransactionsRange(start, end, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Transaction t = Transaction.fromPayload(doc.getData(), pref.getFamilyId(), apiHelper.getContext(), pref.getUserId());
                        if (t != null) transactions.add(t);
                    }
                    transactionDao.insertAll(transactions);
                    Log.d(TAG, "Synced current month items: " + transactions.size());
                });
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "Current month sync failed: " + error.getMessage());
            }
        });
    }

    public void syncMonthForReport(int month, int year, AppwriteManager.AppwriteCallback<Void> callback) {
        String start = Utils.getMonthStart(month, year);
        String end = Utils.getMonthEnd(month, year);

        apiHelper.getTransactionsRange(start, end, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Transaction t = Transaction.fromPayload(doc.getData(), pref.getFamilyId(), apiHelper.getContext(), pref.getUserId());
                        if (t != null) transactions.add(t);
                    }
                    transactionDao.insertAll(transactions);
                    if (callback != null) callback.onSuccess(null);
                });
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void syncIncomes() {
        // Migration: Incomes are now unified into Transactions collection
    }

    public void syncGoals() {
        // Migration: Goals are now unified into Transactions collection
    }

    public void syncDebts() {
        // Migration: Debts are now unified into Transactions collection
    }

    public LiveData<List<TransactionWithUser>> getAllTransactions() {
        return transactionDao.getAllTransactions(pref.getFamilyId(), pref.getUserId());
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByType(pref.getFamilyId(), type, pref.getUserId());
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByMonth(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long end = cal.getTimeInMillis() - 1;
        return transactionDao.getTransactionsInRange(pref.getFamilyId(), start, end, pref.getUserId());
    }

    public LiveData<Double> getTotalIncome() {
        return transactionDao.getTotalIncome(pref.getFamilyId());
    }

    public LiveData<Double> getTotalIncomeInRange(long start, long end) {
        return transactionDao.getTotalIncomeInRange(pref.getFamilyId(), start, end);
    }

    public LiveData<Double> getTotalExpenseInRange(long start, long end) {
        return transactionDao.getTotalExpenseInRange(pref.getFamilyId(), start, end);
    }

    public LiveData<Double> getTodayTotalSpent() {
        return transactionDao.getTodayTotalSpent(pref.getFamilyId(), Utils.getStartOfDayMillis());
    }

    public LiveData<List<TransactionWithUser>> getIncome() {
        return getTransactionsByType("INCOME");
    }

    public LiveData<List<TransactionWithUser>> getGoals() {
        return getTransactionsByType("GOAL");
    }

    public LiveData<List<TransactionWithUser>> getDebts() {
        return getTransactionsByType("DEBT");
    }

    public LiveData<List<TransactionWithUser>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(pref.getFamilyId(), limit, pref.getUserId());
    }

    public LiveData<List<TransactionWithUser>> getTransactionsInRange(long start, long end) {
        return transactionDao.getTransactionsInRange(pref.getFamilyId(), start, end, pref.getUserId());
    }

    public LiveData<TransactionWithUser> getTransactionById(String id) {
        return transactionDao.getTransactionById(id, pref.getUserId());
    }

    public void deleteLocally(String id) {
        executor.execute(() -> transactionDao.deleteById(id));
    }

    // ─── SOCIAL INTERACTIONS ─────────────────────────────────────────────────

    public void likeTransaction(String transactionId, String emoji, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        // Optimistic UI: Update local database immediately
        executor.execute(() -> {
            String myId = pref.getUserId();
            String myUsername = pref.getUsername();
            if (myUsername != null) myUsername = myUsername.toLowerCase(); // Ensure lowercase as per project rule
            
            String tempId = "temp_" + System.currentTimeMillis();

            // 1. Update Likes table
            AppDatabase.getInstance(apiHelper.getContext()).likeDao().deleteUserLike(transactionId, myId);
            AppDatabase.getInstance(apiHelper.getContext()).likeDao().insert(
                    new Like(tempId, transactionId, myId, myUsername, emoji)
            );

            // 2. Optimistically update Transaction table counters/last emoji
            Transaction t = transactionDao.getTransactionByIdRawSync(transactionId);
            if (t != null) {
                t.setLastEmoji(emoji);
                // We don't know the exact new count without fetching all, but we can increment/decrement
                // For simplicity, just update the emoji for now as that's the user's main concern
                transactionDao.insert(t); 
            }
        });

        apiHelper.addLike(transactionId, emoji, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                // Final sync will be handled by Realtime stream which updates both tables
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                // Rollback on error
                syncCurrentMonth();
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void unlikeTransaction(String likeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.removeLike(likeId, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                syncCurrentMonth();
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void postComment(String transactionId, String text, String parentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.addComment(transactionId, text, parentId, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                syncCurrentMonth();
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void removeComment(String commentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.deleteComment(commentId, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                syncCurrentMonth();
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────

    public void addTransaction(Transaction transaction, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", transaction.getAmount());
        data.put("title", transaction.getTitle());
        data.put("category", transaction.getCategory());
        data.put("description", transaction.getDescription());
        data.put("type", transaction.getType());
        data.put("note", transaction.getNote());
        data.put("members", transaction.getMembers());
        data.put("startDate", transaction.getCreatedAt() != null ? transaction.getCreatedAt() : Utils.getCurrentTimestamp());

        if ("INCOME".equals(transaction.getType())) {
            data.put("source", transaction.getTitle());
        } else if ("GOAL".equals(transaction.getType())) {
            data.put("targetAmount", transaction.getTargetAmount());
        } else if ("DEBT".equals(transaction.getType())) {
            data.put("personName", transaction.getTitle());
        } else if ("ALLOCATION".equals(transaction.getType())) {
            data.put("allocatedTo", transaction.getAllocatedTo());
            data.put("allocationMonth", transaction.getAllocationMonth());
        }

        apiHelper.callAction(Constants.Actions.ADD_TRANSACTION, data, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if ("EXPENSE".equals(transaction.getType())) {
                    com.upreyvan.carti.data.local.BudgetManager.getInstance(apiHelper.getContext())
                        .addExpenseToCategory(transaction.getCategory(), transaction.getAmount());
                }
                refreshTransactions();
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void updateTransaction(Transaction transaction, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", transaction.getId());
        data.put("amount", transaction.getAmount());
        data.put("title", transaction.getTitle());
        data.put("category", transaction.getCategory());
        data.put("description", transaction.getDescription());
        data.put("type", transaction.getType());
        data.put("isPaid", transaction.isPaid());

        apiHelper.callAction(Constants.Actions.UPDATE_TRANSACTION, data, callback);
    }

    public void saveLocally(Transaction transaction) {
        executor.execute(() -> {
            transactionDao.insert(transaction);
            refreshTransactions();
        });
    }

}
