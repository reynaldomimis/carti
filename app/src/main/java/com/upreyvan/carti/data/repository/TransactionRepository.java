package com.upreyvan.carti.data.repository;

import android.content.Context;
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
import com.upreyvan.carti.data.remote.RealtimeHelper;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.RealtimeSubscription;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final RealtimeHelper realtimeHelper;
    private RealtimeSubscription realtimeSubscription;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TransactionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        transactionDao = db.transactionDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
        realtimeHelper = new RealtimeHelper(context);
        initRealtime();
    }

    private void initRealtime() {
        if (realtimeSubscription != null) return;
        
        realtimeSubscription = realtimeHelper.subscribeToCollection(
                Constants.Appwrite.COL_TRANSACTIONS,
                event -> refreshTransactions()
        );
    }

    public void onDestroy() {
        if (realtimeSubscription != null) {
            realtimeSubscription.close();
            realtimeSubscription = null;
        }
    }


    public void addTransaction(Transaction transaction, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        AppwriteManager.AppwriteCallback<Map<String, Object>> internalCallback = new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                String id = String.valueOf(result.get("$id"));
                transaction.setId(id);
                saveLocally(transaction);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        };

        switch (transaction.getType()) {
            case "INCOME":
                apiHelper.addIncome(transaction.getTitle(), transaction.getAmount(), internalCallback);
                break;
            case "GOAL":
                apiHelper.addGoal(transaction.getTitle(), transaction.getTargetAmount(), internalCallback);
                break;
            case "DEBT":
                apiHelper.addDebt(transaction.getTitle(), transaction.getAmount(), "DEBT", transaction.getCategory(), transaction.getDueDate(), transaction.getReminder(), transaction.getDescription(), internalCallback);
                break;
            default:
                apiHelper.addTransaction(transaction.getAmount(), transaction.getType(), transaction.getCategory(), transaction.getDescription(), internalCallback);
                break;
        }
    }

    public void updateTransaction(Transaction transaction, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        AppwriteManager.AppwriteCallback<Map<String, Object>> internalCallback = new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                saveLocally(transaction);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        };

        switch (transaction.getType()) {
            case "INCOME":
                apiHelper.updateIncome(transaction.getId(), transaction.getTitle(), transaction.getAmount(), internalCallback);
                break;
            case "GOAL":
                apiHelper.updateGoalAmount(transaction.getId(), transaction.getAmount(), internalCallback);
                break;
            case "DEBT":
                apiHelper.updateDebtAmount(transaction.getId(), transaction.getAmount(), internalCallback);
                break;
            default:
                saveLocally(transaction);
                if (callback != null) callback.onSuccess(null);
                break;
        }
    }

    public void deleteTransaction(Transaction transaction, AppwriteManager.AppwriteCallback<Object> callback) {
        apiHelper.deleteTransaction(transaction.getId(), new AppwriteManager.AppwriteCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                executor.execute(() -> transactionDao.delete(transaction));
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void saveLocally(Transaction transaction) {
        transaction.setFamilyId(pref.getFamilyId());
        if (transaction.getCreatedAt() == null) transaction.setCreatedAt(Utils.getCurrentTimestamp());
        transaction.setUpdatedAt(Utils.getCurrentTimestamp());
        executor.execute(() -> transactionDao.insert(transaction));
    }

    public void deleteLocally(String id) {
        executor.execute(() -> transactionDao.deleteById(id));
    }

    // --- Getters by Type ---

    public LiveData<List<TransactionWithUser>> getIncome() {
        return transactionDao.getTransactionsByType(pref.getFamilyId(), "INCOME");
    }

    public LiveData<List<TransactionWithUser>> getExpenses() {
        return transactionDao.getTransactionsByType(pref.getFamilyId(), "EXPENSE");
    }

    public LiveData<List<TransactionWithUser>> getDebt() {
        return transactionDao.getTransactionsByType(pref.getFamilyId(), "DEBT");
    }

    public LiveData<List<TransactionWithUser>> getGoals() {
        return transactionDao.getTransactionsByType(pref.getFamilyId(), "GOAL");
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByType(pref.getFamilyId(), type);
    }

    public LiveData<List<TransactionWithUser>> getAllTransactions() {
        return transactionDao.getAllTransactions(pref.getFamilyId());
    }

    public LiveData<List<TransactionWithUser>> getTransactionsInRange(long start, long end) {
        return transactionDao.getTransactionsInRange(pref.getFamilyId(), start, end);
    }

    public LiveData<List<TransactionWithUser>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(pref.getFamilyId(), limit);
    }

    public LiveData<Transaction> getTransactionById(String id) {
        return transactionDao.getTransactionByIdRaw(id);
    }

    public LiveData<Double> getTodayTotalSpent() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return transactionDao.getTodayTotalSpent(pref.getFamilyId(), cal.getTimeInMillis());
    }

    public LiveData<Double> getTotalIncome() {
        return transactionDao.getTotalIncome(pref.getFamilyId());
    }

    // --- Sync Logic ---

    public void syncTransactionsIfNeeded() {
        executor.execute(() -> {
            List<Transaction> local = transactionDao.getAllTransactionsList(pref.getFamilyId());
            if (local == null || local.isEmpty()) {
                pref.resetLastSyncTime();
            }
            refreshTransactions();
        });
    }

    public void refreshTransactions() {
        String familyId = pref.getFamilyId();
        String lastSync = pref.getLastSyncTime();

        apiHelper.getTransactionsSince(lastSync, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (result.getDocuments().isEmpty()) return;

                executor.execute(() -> {
                    List<Transaction> transactions = new ArrayList<>();
                    String latestTimestamp = lastSync;

                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        transactions.add(mapToTransaction(doc, familyId));
                        if (doc.getCreatedAt().compareTo(latestTimestamp) > 0) {
                            latestTimestamp = doc.getCreatedAt();
                        }
                    }
                    transactionDao.insertAll(transactions);
                    pref.setLastSyncTime(latestTimestamp);
                });
            }

            @Override
            public void onError(Throwable error) {}
        });
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByMonth(int month, int year) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.YEAR, year);
        cal.set(java.util.Calendar.MONTH, month);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.add(java.util.Calendar.MONTH, 1);
        cal.add(java.util.Calendar.MILLISECOND, -1);
        long end = cal.getTimeInMillis();

        return transactionDao.getTransactionsInRange(pref.getFamilyId(), start, end);
    }

    private Transaction mapToTransaction(Document<Map<String, Object>> doc, String familyId) {
        Map<String, Object> data = doc.getData();
        String type = String.valueOf(data.get("type"));
        double amount = Utils.getDouble(data.get("amount"));
        double targetAmount = Utils.getDouble(data.get("targetAmount"));
        String categoryName = String.valueOf(data.get("category"));
        String userId = String.valueOf(data.get("userId"));
        
        String title = data.containsKey("title") ? String.valueOf(data.get("title")) : 
                     (data.containsKey("name") ? String.valueOf(data.get("name")) : 
                     (data.containsKey("source") ? String.valueOf(data.get("source")) : 
                     (data.containsKey("personName") ? String.valueOf(data.get("personName")) : categoryName)));
        
        String description = data.containsKey("description") ? String.valueOf(data.get("description")) : 
                           (data.containsKey("note") ? String.valueOf(data.get("note")) : 
                           (data.containsKey("notes") ? String.valueOf(data.get("notes")) : ""));
        
        String status = data.containsKey("status") ? String.valueOf(data.get("status")) : "active";
        
        boolean isPaid = false;
        if (data.containsKey("isPaid") && data.get("isPaid") != null) {
            Object val = data.get("isPaid");
            if (val instanceof Boolean) isPaid = (Boolean) val;
        }

        String dueDate = data.containsKey("dueDate") ? String.valueOf(data.get("dueDate")) : 
                       (data.containsKey("targetDate") ? String.valueOf(data.get("targetDate")) : null);
        
        List<String> members = new ArrayList<>();
        if (data.containsKey("members") && data.get("members") instanceof List) {
            List<?> list = (List<?>) data.get("members");
            for (Object item : list) {
                members.add(String.valueOf(item));
            }
        }
        
        int iconRes = R.drawable.ic_person;
        int iconColor = ContextCompat.getColor(apiHelper.getContext(), R.color.carti_primary_green);

        List<com.upreyvan.carti.model.Category> categories = com.upreyvan.carti.data.local.CategoryManager.getInstance(apiHelper.getContext()).getCategories();
        for (com.upreyvan.carti.model.Category cat : categories) {
            if (cat.getName().equalsIgnoreCase(categoryName)) {
                iconRes = cat.getIconRes();
                iconColor = ContextCompat.getColor(apiHelper.getContext(), cat.getIconColor());
                break;
            }
        }

        int bgColor = androidx.core.graphics.ColorUtils.setAlphaComponent(iconColor, 25);
        
        return new Transaction(
            doc.getId(),
            type,
            amount,
            title,
            description,
            categoryName,
            familyId,
            userId,
            doc.getCreatedAt(),
            doc.getUpdatedAt(),
            targetAmount,
            dueDate,
            status,
            isPaid,
            members,
            data.containsKey("reminder") ? String.valueOf(data.get("reminder")) : "",
            iconRes,
            bgColor,
            iconColor,
            Utils.getMillisFromIso(doc.getCreatedAt())
        );
    }
}
