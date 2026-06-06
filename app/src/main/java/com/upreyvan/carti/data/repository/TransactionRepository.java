package com.upreyvan.carti.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.source.TransactionRemoteDataSource;
import com.upreyvan.carti.model.Comment;
import com.upreyvan.carti.model.Like;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.CommentHelper;
import com.upreyvan.carti.util.Utils;

import java.io.File;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private static TransactionRepository instance;

    private final TransactionRemoteDataSource remoteDataSource;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    
    private final MutableLiveData<List<TransactionWithUser>> allTransactions = new MutableLiveData<>(new ArrayList<>());

    private final Map<String, Long> requestCooldowns = new ConcurrentHashMap<>();
    private final java.util.Set<String> pendingToggles = Collections.synchronizedSet(new java.util.HashSet<>());
    private static final long COOLDOWN_MS = 1000;

    private TransactionRepository(Context context) {
        this.remoteDataSource = new TransactionRemoteDataSource(context);
        this.pref = PreferenceManager.getInstance(context);
        refreshTransactions();
    }

    public static synchronized TransactionRepository getInstance(Context context) {
        if (instance == null) instance = new TransactionRepository(context);
        return instance;
    }

    public LiveData<Boolean> getSyncingStatus() { return isSyncing; }

    private void postStableList(List<TransactionWithUser> list) {
        if (list == null) return;
        // Ensure the list is always sorted by timestamp descending (Newest first)
        // This prevents the "jumping" behavior when server syncs out-of-order.
        list.sort((a, b) -> Long.compare(b.getTransaction().getTimestampMillis(), a.getTransaction().getTimestampMillis()));
        allTransactions.postValue(list);
    }

    public void refreshTransactions() {
        if (isRefreshing.getAndSet(true)) return;
        
        requestCooldowns.clear();
        pendingToggles.clear();

        isSyncing.postValue(true);
        remoteDataSource.getTransactionsSince("", new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
                    List<TransactionWithUser> transactions = new ArrayList<>();
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        Transaction t = Utils.parseTransaction(doc.getData(), doc.getId(), doc.getCreatedAt(), doc.getUpdatedAt());
                        TransactionWithUser tu = new TransactionWithUser();
                        tu.setTransaction(t);
                        transactions.add(tu);
                    }
                    postStableList(transactions);
                    pref.setLastSyncTime(Utils.getCurrentTimestamp());
                    pref.setLastSyncTimeMillis(System.currentTimeMillis());
                    
                    for (TransactionWithUser tu : transactions) {
                        if (tu.getTransaction().getLikesCount() > 0) {
                            hydrateMissingReactions(tu.getTransaction().getId());
                        }
                    }

                    isSyncing.postValue(false);
                    isRefreshing.set(false);
                });
            }
            @Override public void onError(Throwable error) {
                Log.e(TAG, "Refresh failed: " + error.getMessage());
                isSyncing.postValue(false);
                isRefreshing.set(false);
            }
        });
    }

    public LiveData<List<TransactionWithUser>> getRecentTransactions(int limit) {
        return Transformations.map(allTransactions, list -> 
            list.stream().limit(limit).collect(Collectors.toList())
        );
    }

    public LiveData<List<TransactionWithUser>> getAllTransactions() { return allTransactions; }

    public LiveData<List<TransactionWithUser>> getTransactionsByType(String type) {
        return Transformations.map(allTransactions, list -> 
            list.stream().filter(tu -> type.equalsIgnoreCase(tu.getTransaction().getType())).collect(Collectors.toList())
        );
    }

    public LiveData<List<TransactionWithUser>> getGoals() { return getTransactionsByType("GOAL"); }
    public LiveData<List<TransactionWithUser>> getIncome() { return getTransactionsByType("INCOME"); }
    public LiveData<List<TransactionWithUser>> getAllocations() { return getTransactionsByType("ALLOCATION"); }

    public LiveData<List<TransactionWithUser>> getAllocationsByMonth(String month) {
        return Transformations.map(allTransactions, list -> 
            list.stream()
                .filter(tu -> "ALLOCATION".equalsIgnoreCase(tu.getTransaction().getType()) && java.util.Objects.equals(month, tu.getTransaction().getAllocationMonth()))
                .collect(Collectors.toList())
        );
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByAllocation(String goalId) {
        return Transformations.map(allTransactions, list -> 
            list.stream()
                .filter(tu -> java.util.Objects.equals(goalId, tu.getTransaction().getAllocatedTo()))
                .collect(Collectors.toList())
        );
    }

    public LiveData<List<TransactionWithUser>> getTransactionsByMonth(int month, int year) {
        Calendar cal = Calendar.getInstance(); cal.set(Calendar.YEAR, year); cal.set(Calendar.MONTH, month);
        long start = Utils.getMonthStartMillis(cal);
        long end = Utils.getMonthEndMillis(cal);
        return Transformations.map(allTransactions, list -> 
            list.stream()
                .filter(tu -> {
                    long t = tu.getTransaction().getTimestampMillis();
                    return t >= start && t <= end;
                })
                .collect(Collectors.toList())
        );
    }

    public LiveData<TransactionWithUser> getTransactionById(String id) {
        return Transformations.map(allTransactions, list -> 
            list.stream().filter(tu -> java.util.Objects.equals(id, tu.getTransaction().getId())).findFirst().orElse(null)
        );
    }

    public void addTransaction(Transaction t, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) {
        remoteDataSource.addTransaction(mapTransactionFields(t), new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(Map<String, Object> result) {
                refreshTransactions();
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Throwable error) { if (callback != null) callback.onError(error); }
        });
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
                refreshTransactions();
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
            @Override public void onSuccess(Object result) { 
                refreshTransactions();
                if (callback != null) callback.onSuccess(result); 
            }
            @Override public void onError(Throwable error) { if (callback != null) callback.onError(error); }
        });
    }

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

    public LiveData<Double> getTotalIncome() {
        return Transformations.map(allTransactions, list -> 
            list.stream()
                .filter(tu -> "INCOME".equalsIgnoreCase(tu.getTransaction().getType()))
                .mapToDouble(tu -> tu.getTransaction().getAmount())
                .sum()
        );
    }

    public LiveData<Double> getTotalExpense() {
        return Transformations.map(allTransactions, list -> 
            list.stream()
                .filter(tu -> "EXPENSE".equalsIgnoreCase(tu.getTransaction().getType()))
                .mapToDouble(tu -> tu.getTransaction().getAmount())
                .sum()
        );
    }

    public LiveData<Double> getBalance() {
        return Transformations.map(allTransactions, list -> {
            double income = list.stream()
                .filter(tu -> "INCOME".equalsIgnoreCase(tu.getTransaction().getType()))
                .mapToDouble(tu -> tu.getTransaction().getAmount())
                .sum();
            double expense = list.stream()
                .filter(tu -> "EXPENSE".equalsIgnoreCase(tu.getTransaction().getType()))
                .mapToDouble(tu -> tu.getTransaction().getAmount())
                .sum();
            return income - expense;
        });
    }

    public LiveData<Double> getTotalIncomeInRange(long start, long end) {
        return Transformations.map(allTransactions, list -> 
            list.stream()
                .filter(tu -> "INCOME".equalsIgnoreCase(tu.getTransaction().getType()) && 
                        tu.getTransaction().getTimestampMillis() >= start && 
                        tu.getTransaction().getTimestampMillis() <= end)
                .mapToDouble(tu -> tu.getTransaction().getAmount())
                .sum()
        );
    }

    public LiveData<Double> getTotalExpenseInRange(long start, long end) {
        return Transformations.map(allTransactions, list -> 
            list.stream()
                .filter(tu -> "EXPENSE".equalsIgnoreCase(tu.getTransaction().getType()) && 
                        tu.getTransaction().getTimestampMillis() >= start && 
                        tu.getTransaction().getTimestampMillis() <= end)
                .mapToDouble(tu -> tu.getTransaction().getAmount())
                .sum()
        );
    }

    public void unlikeTransaction(String likeId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) { remoteDataSource.removeLike(likeId, callback); }
    public void likeTransaction(String transId, String emoji, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) { remoteDataSource.addLike(transId, emoji, callback); }
    public void getLikes(String transId, AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>> callback) { remoteDataSource.getLikes(transId, callback); }
    public void markDebtPaid(String debtId, AppwriteManager.AppwriteCallback<Map<String, Object>> callback) { 
        remoteDataSource.markDebtPaid(debtId, new AppwriteManager.AppwriteCallback<>() {
            @Override public void onSuccess(Map<String, Object> result) {
                refreshTransactions();
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Throwable error) { if (callback != null) callback.onError(error); }
        });
    }

    public void toggleLike(TransactionWithUser item, String emoji) {
        String transId = item.getTransaction().getId();
        String myUserId = pref.getUserId();
        
        long now = System.currentTimeMillis();
        Long lastRequest = requestCooldowns.get(transId);
        if ((lastRequest != null && now - lastRequest < COOLDOWN_MS) || pendingToggles.contains(transId)) {
            return;
        }
        
        requestCooldowns.put(transId, now);
        pendingToggles.add(transId);

        boolean isUnlike = emoji.equals(item.getMyReaction());
        String currentLikeId = item.getMyLikeId();

        applyOptimisticLike(transId, myUserId, isUnlike, emoji);

        if (isUnlike && currentLikeId != null && !currentLikeId.isEmpty()) {
            unlikeTransaction(currentLikeId, new AppwriteManager.AppwriteCallback<>() {
                @Override public void onSuccess(Map<String, Object> result) {
                    pendingToggles.remove(transId);
                }
                @Override public void onError(Throwable error) {
                    pendingToggles.remove(transId);
                    refreshTransactions();
                }
            });
        } else {
            likeTransaction(transId, emoji, new AppwriteManager.AppwriteCallback<>() {
                @Override public void onSuccess(Map<String, Object> result) {
                    pendingToggles.remove(transId);
                }
                @Override public void onError(Throwable error) {
                    pendingToggles.remove(transId);
                    refreshTransactions();
                }
            });
        }
    }

    private void applyOptimisticLike(String transId, String userId, boolean isUnlike, String emoji) {
        List<TransactionWithUser> current = allTransactions.getValue();
        if (current == null) return;

        List<TransactionWithUser> updated = new ArrayList<>();
        boolean found = false;
        for (TransactionWithUser tu : current) {
                if (tu.getTransaction().getId().equals(transId)) {
                TransactionWithUser tuCopy = tu.copy();
                Transaction t = tuCopy.getTransaction();
                List<Like> reactions = tuCopy.getReactions() != null ? new ArrayList<>(tuCopy.getReactions()) : new ArrayList<>();
                
                // Fix: Nuclear De-duplication (userId OR username)
                String myUsername = pref.getUsername();
                reactions.removeIf(l -> userId.equals(l.getUserId()) || myUsername.equalsIgnoreCase(l.getUsername()));

                if (isUnlike) {
                    tuCopy.setMyReaction(null);
                    tuCopy.setMyLikeId(null);
                    } else {
                        reactions.add(new Like("temp_" + UUID.randomUUID(), transId, userId, myUsername, emoji));
                        tuCopy.setMyReaction(emoji);
                    }
                tuCopy.setReactions(reactions);
                t.setLikesCount(reactions.size()); // Fix A: Computed Truth
                updated.add(tuCopy);
                found = true;
            } else {
                updated.add(tu);
            }
        }
        if (found) postStableList(updated);
    }

    public void handleLikeEventLocally(Map<String, Object> payload, boolean isDelete) {
        String transId = (String) payload.get("transactionId");
        String likeId = (String) payload.get("$id");
        String userId = (String) payload.get("userId");
        String updatedAt = (String) payload.get("$updatedAt");

        if (transId == null || likeId == null || userId == null) return;

        if (userId.equals(pref.getUserId()) && pendingToggles.contains(transId)) {
            Log.d(TAG, "Ignoring server event: Local intent is still authoritative.");
            return;
        }

        executor.execute(() -> {
            List<TransactionWithUser> current = allTransactions.getValue();
            if (current == null) return;

            List<TransactionWithUser> updated = new ArrayList<>();
            boolean modified = false;

            for (TransactionWithUser tu : current) {
                if (tu.getTransaction().getId().equals(transId)) {
                    if (tu.getTransaction().getUpdatedAt() != null && updatedAt != null) {
                        if (updatedAt.compareTo(tu.getTransaction().getUpdatedAt()) < 0) {
                            Log.d(TAG, "Discarding stale realtime event (Older version).");
                            updated.add(tu);
                            continue;
                        }
                    }

                    TransactionWithUser tuCopy = tu.copy();
                    tuCopy.getTransaction().setUpdatedAt(updatedAt);
                    List<Like> reactions = tuCopy.getReactions() != null ? new ArrayList<>(tuCopy.getReactions()) : new ArrayList<>();

                    if (isDelete) {
                        // Nuclear Purge (by ID, userId, or username) to prevent ghosts
                        String myUsername = pref.getUsername();
                        if (reactions.removeIf(l -> likeId.equals(l.getId()) || userId.equals(l.getUserId()) || (userId.equals(pref.getUserId()) && myUsername.equalsIgnoreCase(l.getUsername())))) {
                            tuCopy.setReactions(reactions);
                            tuCopy.getTransaction().setLikesCount(reactions.size());
                            if (pref.getUserId().equals(userId)) {
                                tuCopy.setMyReaction(null);
                                tuCopy.setMyLikeId(null);
                            }
                        }
                    } else {
                        String emoji = (String) payload.get("emojiType");
                        String username = (String) payload.get("username");

                        // Nuclear De-duplication before injection
                        reactions.removeIf(l -> userId.equals(l.getUserId()) || username.equalsIgnoreCase(l.getUsername()));
                        
                        Like newLike = new Like(likeId, transId, userId, username, emoji);
                        newLike.setUpdatedAt(updatedAt);
                        reactions.add(newLike);
                        
                        tuCopy.setReactions(reactions);
                        tuCopy.getTransaction().setLikesCount(reactions.size()); // Fix A: Computed Truth

                        if (pref.getUserId().equals(userId)) {
                            tuCopy.setMyReaction(emoji);
                            tuCopy.setMyLikeId(likeId);
                        }
                    }

                    tuCopy.setReactions(reactions);
                    updated.add(tuCopy);
                    modified = true;
                } else {
                    updated.add(tu);
                }
            }
            
            if (modified) {
                postStableList(updated);
            } else {
                hydrateMissingReactions(transId);
            }
        });
    }

    private void hydrateMissingReactions(String transId) {
        getLikes(transId, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                executor.execute(() -> {
                    List<TransactionWithUser> current = allTransactions.getValue();
                    if (current == null) return;
                    List<TransactionWithUser> updated = new ArrayList<>();
                    boolean found = false;
                    for (TransactionWithUser tu : current) {
                        if (tu.getTransaction().getId().equals(transId)) {
                            TransactionWithUser tuCopy = tu.copy();
                            List<Like> currentReactions = tuCopy.getReactions() != null ? new ArrayList<>(tuCopy.getReactions()) : new ArrayList<>();

                            for (Document<Map<String, Object>> doc : result.getDocuments()) {
                                String sUserId = (String) doc.getData().get("userId");
                                String sUsername = (String) doc.getData().get("username");
                                String sUpdatedAt = doc.getUpdatedAt();
                                if (sUserId == null || sUsername == null) continue;

                                boolean hasNewerLocal = false;
                                for (Like existing : currentReactions) {
                                    if (sUserId.equals(existing.getUserId()) || sUsername.equalsIgnoreCase(existing.getUsername())) {
                                        if (existing.getUpdatedAt() != null && sUpdatedAt != null) {
                                            if (sUpdatedAt.compareTo(existing.getUpdatedAt()) < 0) {
                                                hasNewerLocal = true;
                                            }
                                        }
                                        break;
                                    }
                                }

                                if (!hasNewerLocal) {
                                    currentReactions.removeIf(l -> sUserId.equals(l.getUserId()) || sUsername.equalsIgnoreCase(l.getUsername()));
                                    Like newLike = new Like(doc.getId(), transId, sUserId, sUsername, (String) doc.getData().get("emojiType"));
                                    newLike.setUpdatedAt(sUpdatedAt);
                                    currentReactions.add(newLike);
                                }
                            }
                            
                            tuCopy.setReactions(currentReactions);
                            tuCopy.getTransaction().setLikesCount(currentReactions.size());

                            for (Like l : currentReactions) {
                                if (pref.getUserId().equals(l.getUserId())) {
                                    tuCopy.setMyReaction(l.getEmojiType());
                                    tuCopy.setMyLikeId(l.getId());
                                }
                            }

                            updated.add(tuCopy);
                            found = true;
                        } else {
                            updated.add(tu);
                        }
                    }
                    if (found) postStableList(updated);
                });
            }
            @Override public void onError(Throwable e) { Log.e(TAG, "Hydration failed", e); }
        });
    }

    public void handleCommentEventLocally(Map<String, Object> payload, boolean isDelete) {
        String transId = (String) payload.get("transactionId");
        if (transId == null) return;

        executor.execute(() -> {
            List<TransactionWithUser> current = allTransactions.getValue();
            if (current == null) return;

            List<TransactionWithUser> updated = new ArrayList<>();
            boolean modified = false;

            for (TransactionWithUser tu : current) {
                if (tu.getTransaction().getId().equals(transId)) {
                    TransactionWithUser tuCopy = tu.copy();
                    Transaction t = tuCopy.getTransaction();
                    if (isDelete) {
                        t.setCommentCount(Math.max(0, t.getCommentCount() - 1));
                    } else {
                        t.setCommentCount(t.getCommentCount() + 1);
                    }
                    updated.add(tuCopy);
                    modified = true;
                } else {
                    updated.add(tu);
                }
            }
            if (modified) postStableList(updated);
        });
    }

    public void uploadIcon(File file, AppwriteManager.AppwriteCallback<io.appwrite.models.File> callback) {
        AppwriteManager.getInstance(pref.getContext()).uploadFile(
                com.upreyvan.carti.util.Constants.Appwrite.BUCKET_ICONS,
                io.appwrite.ID.Companion.unique(0),
                io.appwrite.models.InputFile.Companion.fromFile(file),
                null,
                callback
        );
    }

    public static class CategorySum {
        public String category;
        public double total;
        public CategorySum(String category, double total) {
            this.category = category;
            this.total = total;
        }
    }
}
