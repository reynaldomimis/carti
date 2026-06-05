package com.upreyvan.carti.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.LikeDao;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.data.local.source.TransactionLocalDataSource;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.source.TransactionRemoteDataSource;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.model.Like;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.CommentHelper;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private static TransactionRepository instance;

    private final TransactionLocalDataSource localDataSource;
    private final TransactionRemoteDataSource remoteDataSource;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);

    private final Map<String, Long> requestCooldowns = new ConcurrentHashMap<>();
    private final java.util.Set<String> pendingToggles = Collections.synchronizedSet(new java.util.HashSet<>());
    private static final long COOLDOWN_MS = 1000;

    private TransactionRepository(Context context) {
        this.localDataSource = new TransactionLocalDataSource(context);
        this.remoteDataSource = new TransactionRemoteDataSource(context);
        this.pref = PreferenceManager.getInstance(context);
    }

    public static synchronized TransactionRepository getInstance(Context context) {
        if (instance == null) instance = new TransactionRepository(context);
        return instance;
    }

    public LiveData<Boolean> getSyncingStatus() { return isSyncing; }

    public void syncTransactionsIfNeeded() {
        executor.execute(() -> {
            boolean isEmpty = localDataSource.getAllTransactionsList(pref.getFamilyId()).isEmpty();
            if (isEmpty || System.currentTimeMillis() - pref.getLastSyncTimeMillis() > 60000) {
                performIncrementalSync(isEmpty);
            }
        });
    }

    public void performIncrementalSync() {
        performIncrementalSync(false);
    }

    public void performIncrementalSync(boolean forceFull) {
        isSyncing.postValue(true);
        String lastSync = forceFull ? "" : pref.getLastSyncTime();
        remoteDataSource.getTransactionsSince(lastSync, new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(DocumentList<Map<String, Object>> result) {
                processAndSaveSync(result);
                if (!result.getDocuments().isEmpty()) {
                    pref.setLastSyncTime(Utils.getCurrentTimestamp());
                    pref.setLastSyncTimeMillis(System.currentTimeMillis());
                }
                isSyncing.postValue(false);
            }
            @Override public void onError(Throwable error) {
                Log.e(TAG, "Sync failed: " + error.getMessage());
                isSyncing.postValue(false);
            }
        });
    }

    private void processAndSaveSync(DocumentList<Map<String, Object>> result) {
        executor.execute(() -> {
            List<Transaction> transactions = new ArrayList<>();
            for (Document<Map<String, Object>> doc : result.getDocuments()) {
                transactions.add(Utils.parseTransaction(doc.getData(), doc.getId(), doc.getCreatedAt(), doc.getUpdatedAt()));
            }
            if (!transactions.isEmpty()) localDataSource.saveTransactions(transactions);
        });
    }

    public LiveData<List<TransactionWithUser>> getRecentTransactions(int limit) { return localDataSource.getRecentTransactions(pref.getFamilyId(), pref.getUserId(), limit); }
    public LiveData<List<TransactionWithUser>> getAllTransactions() { return localDataSource.getAllTransactions(pref.getFamilyId(), pref.getUserId()); }
    public LiveData<List<TransactionWithUser>> getTransactionsByType(String type) { return localDataSource.getTransactionsByType(pref.getFamilyId(), type, pref.getUserId()); }
    public LiveData<List<TransactionWithUser>> getGoals() { return getTransactionsByType("GOAL"); }
    public LiveData<List<TransactionWithUser>> getIncome() { return getTransactionsByType("INCOME"); }
    public LiveData<List<TransactionWithUser>> getTransactionsByAllocation(String goalId) { return localDataSource.getTransactionsByAllocation(pref.getFamilyId(), goalId, pref.getUserId()); }

    public LiveData<List<TransactionWithUser>> getTransactionsByMonth(int month, int year) {
        Calendar cal = Calendar.getInstance(); cal.set(Calendar.YEAR, year); cal.set(Calendar.MONTH, month);
        return localDataSource.getTransactionsInRange(pref.getFamilyId(), Utils.getMonthStartMillis(cal), Utils.getMonthEndMillis(cal), pref.getUserId());
    }

    public LiveData<TransactionWithUser> getTransactionById(String id) { return localDataSource.getTransactionById(id, pref.getUserId()); }

    public void addTransaction(Transaction t, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.addTransaction(mapTransactionFields(t), callback);
    }

    public void addTransaction(double amount, String type, String category, String note, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setType(type);
        t.setCategory(category);
        t.setTitle(note != null && !note.isEmpty() ? note : category);
        t.setNote(note);
        addTransaction(t, callback);
    }

    public void updateTransaction(Transaction t, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.updateTransaction(t.getId(), mapTransactionFields(t), new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(Map<String, Object> result) {
                executor.execute(() -> localDataSource.saveTransactions(Collections.singletonList(t)));
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Throwable error) { if (callback != null) callback.onError(error); }
        });
    }

    private Map<String, Object> mapTransactionFields(Transaction t) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("amount", t.getAmount());
        fields.put("type", t.getType());
        fields.put("title", t.getTitle());
        fields.put("note", t.getNote());
        fields.put("userId", pref.getUserId());
        fields.put("familyId", pref.getFamilyId());
        fields.put("username", pref.getUsername());
        fields.put("startDate", t.getStartDate() != null ? t.getStartDate() : Utils.getCurrentTimestamp());

        String type = t.getType() != null ? t.getType().toUpperCase() : "EXPENSE";

        switch (type) {
            case "INCOME" -> {
                fields.put("category", t.getCategory() != null ? t.getCategory() : "Income");
                fields.put("iconRes", t.getIconRes());
                fields.put("iconUrl", t.getIconUrl());
            }
            case "ALLOCATION" -> {
                fields.put("category", t.getCategory() != null ? t.getCategory() : "Allocation");
                fields.put("allocatedTo", t.getAllocatedTo());
                fields.put("allocationMonth", t.getAllocationMonth());
            }
            case "GOAL" -> {
                fields.put("targetAmount", t.getTargetAmount());
                fields.put("targetDate", t.getTargetDate());
                fields.put("status", t.getStatus() != null ? t.getStatus() : "active");
                fields.put("iconRes", t.getIconRes());
                fields.put("iconUrl", t.getIconUrl());
            }
            case "DEBT" -> {
                fields.put("category", t.getCategory() != null ? t.getCategory() : "Debt");
                fields.put("targetDate", t.getTargetDate());
                fields.put("isPaid", t.isPaid());
                fields.put("status", t.getStatus() != null ? t.getStatus() : "unpaid");
            }
            default -> {
                fields.put("category", t.getCategory() != null ? t.getCategory() : "General");
                fields.put("iconRes", t.getIconRes());
                fields.put("iconUrl", t.getIconUrl());
            }
        }

        if (t.getMembers() != null && !t.getMembers().isEmpty()) {
            fields.put("members", t.getMembers());
        }

        return fields;
    }

    public void deleteTransaction(String id, AppwriteManager.AppwriteCallback<Object> callback) {
        remoteDataSource.deleteTransaction(id, new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(Object result) { deleteLocally(id); if (callback != null) callback.onSuccess(result); }
            @Override public void onError(Throwable error) { if (callback != null) callback.onError(error); }
        });
    }

    public void deleteLocally(String id) { executor.execute(() -> localDataSource.deleteTransactionById(id)); }
    public void postComment(String transactionId, String text, String parentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) { remoteDataSource.addComment(transactionId, text, parentId, callback); }
    public void removeComment(String commentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) { remoteDataSource.deleteComment(commentId, callback); }

    public void getComments(String transactionId, AppwriteManager.AppwriteCallback<List<Comment>> callback) {
        remoteDataSource.getComments(transactionId, new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(DocumentList<Map<String, Object>> result) {
                List<Comment> comments = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) comments.add(CommentHelper.parse(doc.getData()));
                callback.onSuccess(comments);
            }
            @Override public void onError(Throwable error) { callback.onError(error); }
        });
    }

    public void refreshTransactions() { performIncrementalSync(); }
    public LiveData<Double> getTotalIncome() { return AppDatabase.getInstance(pref.getContext()).transactionDao().getTotalIncome(pref.getFamilyId()); }
    public LiveData<Double> getTotalExpense() { return AppDatabase.getInstance(pref.getContext()).transactionDao().getTotalExpense(pref.getFamilyId()); }
    public LiveData<Double> getBalance() { return AppDatabase.getInstance(pref.getContext()).transactionDao().getBalance(pref.getFamilyId()); }
    public LiveData<Double> getTotalIncomeInRange(long start, long end) { return AppDatabase.getInstance(pref.getContext()).transactionDao().getTotalIncomeInRange(pref.getFamilyId(), start, end); }
    public LiveData<Double> getTotalExpenseInRange(long start, long end) { return AppDatabase.getInstance(pref.getContext()).transactionDao().getTotalExpenseInRange(pref.getFamilyId(), start, end); }
    public void unlikeTransaction(String likeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) { remoteDataSource.removeLike(likeId, callback); }
    public void likeTransaction(String transId, String emoji, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) { remoteDataSource.addLike(transId, emoji, callback); }

    public void toggleLike(TransactionWithUser item, String emoji) {
        String myId = pref.getUserId(); 
        String transId = item.getTransaction().getId();
        String myUsername = pref.getUsername();
        
        // Anti-Spam & Request Locking
        long now = System.currentTimeMillis();
        Long lastRequest = requestCooldowns.get(transId);
        if ((lastRequest != null && now - lastRequest < COOLDOWN_MS) || pendingToggles.contains(transId)) {
            Log.d(TAG, "Like toggle throttled or pending for: " + transId);
            return;
        }
        
        requestCooldowns.put(transId, now);
        pendingToggles.add(transId);
        
        executor.execute(() -> {
            try {
                LikeDao likeDao = AppDatabase.getInstance(pref.getContext()).likeDao();
                TransactionDao transDao = AppDatabase.getInstance(pref.getContext()).transactionDao();
                
                Like existing = likeDao.getLikeByUserAndTransaction(transId, myId);
                Transaction trans = transDao.getTransactionByIdRawSync(transId);

                if (existing != null && Objects.equals(existing.getEmojiType(), emoji)) {
                    // UNLIKE
                    likeDao.deleteUserLike(transId, myId);
                    if (trans != null) {
                        trans.setLikesCount(Math.max(0, trans.getLikesCount() - 1));
                        transDao.insert(trans);
                    }
                    unlikeTransaction(existing.getId(), createToggleCallback(transId));
                } else {
                    // LIKE or CHANGE REACTION
                    String likeId = (existing != null) ? existing.getId() : "like_" + UUID.randomUUID().toString().substring(0, 8);
                    Like newLike = new Like(likeId, transId, myId, myUsername, emoji);
                    
                    likeDao.insert(newLike);
                    if (trans != null && existing == null) { // Only increment if it's a new like, not a change
                        trans.setLikesCount(trans.getLikesCount() + 1);
                        transDao.insert(trans);
                    }
                    likeTransaction(transId, emoji, createToggleCallback(transId));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to toggle reaction: " + e.getMessage());
                pendingToggles.remove(transId);
            }
        });
    }

    private AppwriteManager.AppwriteCallback<Map<String, Object>> createToggleCallback(String transId) {
        return new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(Map<String, Object> result) { pendingToggles.remove(transId); }
            @Override public void onError(Throwable error) { 
                pendingToggles.remove(transId);
                Log.e(TAG, "Remote toggle failed for " + transId + ": " + error.getMessage());
            }
        };
    }
}
