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


    public void onDestroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public void syncTransactionsIfNeeded() {
        syncCurrentMonth();
        syncIncomes();
        syncGoals();
        syncDebts();
    }

    public void refreshTransactions() {
        syncTransactionsIfNeeded();
    }

    /**
     * SYNC STRATEGY: Only fetch the current month's data to save bandwidth.
     * If it's a new month, previous data stays in Room (for reports) but sync focus is now.
     */
    public void syncCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        String start = Utils.getMonthStart(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR));
        String end = Utils.getMonthEnd(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR));

        apiHelper.getTransactionsRange(start, end, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (executor.isShutdown()) return;
                executor.execute(() -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        transactions.add(mapPayloadToTransaction(doc.getData(), pref.getFamilyId()));
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

    /**
     * Sync a specific month for report comparison (Last Month vs Now).
     */
    public void syncMonthForReport(int month, int year, AppwriteManager.AppwriteCallback<Void> callback) {
        String start = Utils.getMonthStart(month, year);
        String end = Utils.getMonthEnd(month, year);

        apiHelper.getTransactionsRange(start, end, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (executor.isShutdown()) return;
                executor.execute(() -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        transactions.add(mapPayloadToTransaction(doc.getData(), pref.getFamilyId()));
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
        return transactionDao.getAllTransactions(pref.getFamilyId());
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByType(pref.getFamilyId(), type);
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByMonth(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long end = cal.getTimeInMillis() - 1;
        return transactionDao.getTransactionsInRange(pref.getFamilyId(), start, end);
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
        return transactionDao.getRecentTransactions(pref.getFamilyId(), limit);
    }

    public LiveData<List<TransactionWithUser>> getTransactionsInRange(long start, long end) {
        return transactionDao.getTransactionsInRange(pref.getFamilyId(), start, end);
    }

    public LiveData<TransactionWithUser> getTransactionById(String id) {
        return transactionDao.getTransactionById(id);
    }

    public void deleteLocally(String id) {
        executor.execute(() -> transactionDao.deleteById(id));
    }

    // ─── SOCIAL INTERACTIONS ─────────────────────────────────────────────────

    public void likeTransaction(String transactionId, String emoji, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        apiHelper.addLike(transactionId, emoji, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                syncCurrentMonth(); // Refresh to update counters locally
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
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
        }

        apiHelper.callAction(Constants.Actions.ADD_TRANSACTION, data, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
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
            // Refresh to ensure all observers are notified of the new state
            refreshTransactions();
        });
    }

    private Transaction mapPayloadToTransaction(Map<String, Object> data, String familyId) {
        try {
            String id = String.valueOf(data.get("$id"));
            String createdAt = data.containsKey("startDate") ? String.valueOf(data.get("startDate")) : String.valueOf(data.get("$createdAt"));
            String updatedAt = String.valueOf(data.get("$updatedAt"));
            
            String type = "EXPENSE";
            if (data.containsKey("type")) type = String.valueOf(data.get("type"));
            else if (data.containsKey("source")) type = "INCOME";
            else if (data.containsKey("targetAmount")) type = "GOAL";
            else if (data.containsKey("personName")) type = "DEBT";

            double amount = Utils.getDouble(data.get("amount"));
            double targetAmount = Utils.getDouble(data.get("targetAmount"));
            String categoryName = data.containsKey("category") ? String.valueOf(data.get("category")) : type;
            
            // Fix: Get userId from payload or fallback to current user if it's a local creation
            String userId = data.containsKey("userId") ? String.valueOf(data.get("userId")) : pref.getUserId();
            
            String title = data.containsKey("title") ? String.valueOf(data.get("title")) : 
                         (data.containsKey("username") ? String.valueOf(data.get("username")) : 
                         (data.containsKey("name") ? String.valueOf(data.get("name")) : 
                         (data.containsKey("source") ? String.valueOf(data.get("source")) : categoryName)));
            
            String description = data.containsKey("description") ? String.valueOf(data.get("description")) : 
                               (data.containsKey("note") ? String.valueOf(data.get("note")) : "");
            
            boolean isPaid = false;
            if (data.containsKey("isPaid")) isPaid = (Boolean) data.get("isPaid");

            List<String> members = new ArrayList<>();
            if (data.get("members") instanceof List) {
                for (Object item : (List<?>) data.get("members")) members.add(String.valueOf(item));
            }

            int likesCount = data.containsKey("likesCount") ? ((Number) data.get("likesCount")).intValue() : 0;
            int commentCount = data.containsKey("commentCount") ? ((Number) data.get("commentCount")).intValue() : 0;
            
            int iconRes = R.drawable.ic_person;
            int iconColor = ContextCompat.getColor(apiHelper.getContext(), R.color.carti_primary_green);

            if ("INCOME".equals(type)) { iconRes = R.drawable.ic_arrow_up; iconColor = ContextCompat.getColor(apiHelper.getContext(), R.color.dash_green); }
            else if ("GOAL".equals(type)) { iconRes = R.drawable.ic_trophy; iconColor = ContextCompat.getColor(apiHelper.getContext(), R.color.mint_green); }
            else if ("DEBT".equals(type)) { iconRes = R.drawable.ic_lock; iconColor = ContextCompat.getColor(apiHelper.getContext(), R.color.status_red); }

            int bgColor = androidx.core.graphics.ColorUtils.setAlphaComponent(iconColor, 25);
            
            return new Transaction(
                id, type, amount, title, description, categoryName, familyId, userId,
                createdAt, updatedAt, targetAmount, null, "completed", isPaid, members,
                null, iconRes, bgColor, iconColor, Utils.getMillisFromIso(createdAt),
                likesCount, commentCount
            );
        } catch (Exception e) {
            return null;
        }
    }
}
