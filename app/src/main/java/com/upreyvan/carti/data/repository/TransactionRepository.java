package com.upreyvan.carti.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
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

    private TransactionRepository(Context context) {
        this.localDataSource = new TransactionLocalDataSource(context);
        this.remoteDataSource = new TransactionRemoteDataSource(context);
        this.pref = PreferenceManager.getInstance(context);
    }

    public static synchronized TransactionRepository getInstance(Context context) {
        if (instance == null) instance = new TransactionRepository(context);
        return instance;
    }

    public LiveData<Boolean> getSyncingStatus() {
        return isSyncing;
    }

    public void syncTransactionsIfNeeded() {
        if (System.currentTimeMillis() - pref.getLastSyncTimeMillis() > 60000) {
            performIncrementalSync();
        }
    }

    public void performIncrementalSync() {
        isSyncing.postValue(true);
        remoteDataSource.getTransactionsSince(pref.getLastSyncTime(), new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                processAndSaveSync(result);
                pref.setLastSyncTime(Utils.getCurrentTimestamp());
                pref.setLastSyncTimeMillis(System.currentTimeMillis());
                isSyncing.postValue(false);
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "Incremental sync failed: " + error.getMessage());
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

    public LiveData<List<TransactionWithUser>> getRecentTransactions(int limit) {
        return localDataSource.getRecentTransactions(pref.getFamilyId(), pref.getUserId(), limit);
    }

    public LiveData<List<TransactionWithUser>> getAllTransactions() {
        return localDataSource.getAllTransactions(pref.getFamilyId(), pref.getUserId());
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByType(String type) {
        return localDataSource.getTransactionsByType(pref.getFamilyId(), type, pref.getUserId());
    }

    public LiveData<List<TransactionWithUser>> getGoals() {
        return getTransactionsByType("GOAL");
    }

    public LiveData<List<TransactionWithUser>> getIncome() {
        return getTransactionsByType("INCOME");
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByAllocation(String goalId) {
        return localDataSource.getTransactionsByAllocation(pref.getFamilyId(), goalId, pref.getUserId());
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByMonth(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        long start = Utils.getMonthStartMillis(cal);
        long end = Utils.getMonthEndMillis(cal);
        return localDataSource.getTransactionsInRange(pref.getFamilyId(), start, end, pref.getUserId());
    }

    public LiveData<TransactionWithUser> getTransactionById(String id) {
        return localDataSource.getTransactionById(id, pref.getUserId());
    }

    public void addTransaction(Transaction transaction, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        addTransaction(transaction.getAmount(), transaction.getType(), transaction.getCategory(), transaction.getTitle(), callback);
    }

    public void addTransaction(double amount, String type, String category, String description, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.addTransaction(amount, type, category, description, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void updateTransaction(Transaction transaction, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", transaction.getTitle());
        data.put("amount", transaction.getAmount());
        data.put("targetAmount", transaction.getTargetAmount());
        data.put("dueDate", transaction.getDueDate());
        data.put("members", transaction.getMembers());
        data.put("type", transaction.getType());

        remoteDataSource.updateTransaction(transaction.getId(), data, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                executor.execute(() -> localDataSource.saveTransactions(Collections.singletonList(transaction)));
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void deleteTransaction(String id, AppwriteManager.AppwriteCallback<Object> callback) {
        remoteDataSource.deleteTransaction(id, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Object result) {
                deleteLocally(id);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void deleteLocally(String id) {
        executor.execute(() -> localDataSource.deleteTransactionById(id));
    }

    public void postComment(String transactionId, String text, String parentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.addComment(transactionId, text, parentId, callback);
    }

    public void removeComment(String commentId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.deleteComment(commentId, callback);
    }

    public void getComments(String transactionId, AppwriteManager.AppwriteCallback<List<Comment>> callback) {
        remoteDataSource.getComments(transactionId, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                List<Comment> comments = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    comments.add(CommentHelper.parse(doc.getData()));
                }
                callback.onSuccess(comments);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void refreshTransactions() { performIncrementalSync(); }

    public LiveData<Double> getTotalIncome() {
        return AppDatabase.getInstance(pref.getContext()).transactionDao().getTotalIncome(pref.getFamilyId());
    }

    public LiveData<Double> getTotalIncomeInRange(long start, long end) {
        return AppDatabase.getInstance(pref.getContext()).transactionDao().getTotalIncomeInRange(pref.getFamilyId(), start, end);
    }

    public LiveData<Double> getTotalExpenseInRange(long start, long end) {
        return AppDatabase.getInstance(pref.getContext()).transactionDao().getTotalExpenseInRange(pref.getFamilyId(), start, end);
    }
    
    public void unlikeTransaction(String likeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.removeLike(likeId, callback);
    }

    public void likeTransaction(String transId, String emoji, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.addLike(transId, emoji, callback);
    }

    public void toggleLike(TransactionWithUser item, String emoji) {
        String myId = pref.getUserId();
        String myReaction = item.getMyReaction();
        String transId = item.getTransaction().getId();

        if (Objects.equals(myReaction, emoji)) {
            executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(pref.getContext());
                List<Like> likes = db.likeDao().getLikesForTransaction(transId);
                String targetId = null;
                for (Like l : likes) {
                    if (Objects.equals(l.getUserId(), myId)) {
                        targetId = l.getId();
                        break;
                    }
                }
                if (targetId != null) unlikeTransaction(targetId, null);
            });
        } else {
            likeTransaction(transId, emoji, null);
        }
    }
}
