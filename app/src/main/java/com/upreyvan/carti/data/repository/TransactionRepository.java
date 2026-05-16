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
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final ApiHelper apiHelper;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TransactionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        transactionDao = db.transactionDao();
        apiHelper = new ApiHelper(context);
        pref = new PreferenceManager(context);
    }

    public LiveData<List<Transaction>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(pref.getFamilyId(), limit);
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions(pref.getFamilyId());
    }

    public LiveData<List<Transaction>> getTransactionsInRange(long start, long end) {
        return transactionDao.getTransactionsInRange(pref.getFamilyId(), start, end);
    }

    public LiveData<Double> getTodayTotalSpent() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return transactionDao.getTodayTotalSpent(pref.getFamilyId(), cal.getTimeInMillis());
    }

    public void syncTransactionsIfNeeded() {
        // Delta Sync: Check for updates every time the app opens/needed
        refreshTransactions();
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
                        
                        // Keep track of the most recent record's timestamp
                        if (doc.getCreatedAt().compareTo(latestTimestamp) > 0) {
                            latestTimestamp = doc.getCreatedAt();
                        }
                    }
                    
                    // Insert new/updated data without deleting the whole database
                    transactionDao.insertAll(transactions);
                    
                    // Save the timestamp for the next sync
                    pref.setLastSyncTime(latestTimestamp);
                });
            }

            @Override
            public void onError(Throwable error) {}
        });
    }

    public void saveLocally(Transaction transaction) {
        transaction.setFamilyId(pref.getFamilyId());
        executor.execute(() -> transactionDao.insert(transaction));
    }

    public void deleteLocally(String id) {
        executor.execute(() -> transactionDao.deleteById(id));
    }

    private Transaction mapToTransaction(Document<Map<String, Object>> doc, String familyId) {
        Map<String, Object> data = doc.getData();
        String type = String.valueOf(data.get("type"));
        double amount = Utils.getDouble(data.get("amount"));
        String categoryName = String.valueOf(data.get("category"));
        
        // Better mapping logic for icons based on category
        int iconRes = R.drawable.ic_person;
        int iconColor = ContextCompat.getColor(apiHelper.getContext(), R.color.carti_primary_green);

        // Try to match with existing categories for better visuals
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
            familyId,
            categoryName,
            Utils.formatTimestamp(doc.getCreatedAt()),
            String.format(Locale.US, "₱%,.2f", amount),
            iconRes,
            bgColor,
            iconColor,
            Utils.getMillisFromIso(doc.getCreatedAt()),
            type
        );
    }
}