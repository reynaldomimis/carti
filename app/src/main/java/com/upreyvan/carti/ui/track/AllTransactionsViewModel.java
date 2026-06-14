package com.upreyvan.carti.ui.track;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 4: Centralized All Transactions Logic.
 * Handles filtering, searching, and pagination on a background thread.
 */
public class AllTransactionsViewModel extends BaseViewModel {
    private final TransactionRepository repo;
    
    private final MutableLiveData<FilterState> filterState = new MutableLiveData<>(new FilterState(null, null, null, ""));
    private final MediatorLiveData<List<TransactionWithUser>> filteredTransactions = new MediatorLiveData<>();

    public AllTransactionsViewModel(@NonNull Application application) {
        super(application);
        this.repo = TransactionRepository.getInstance(application);
        
        // Observe both repository changes and filter changes
        filteredTransactions.addSource(repo.getAllTransactions(), transactions -> applyFilter());
        filteredTransactions.addSource(filterState, state -> applyFilter());
    }

    public LiveData<List<TransactionWithUser>> getFilteredTransactions() {
        return filteredTransactions;
    }

    public void setFilters(String type, String userId, String category, String query) {
        filterState.setValue(new FilterState(type, userId, category, query != null ? query.toLowerCase().trim() : ""));
    }

    public void setSearchQuery(String query) {
        FilterState current = filterState.getValue();
        if (current != null) {
            setFilters(current.type, current.userId, current.category, query);
        }
    }

    private void applyFilter() {
        List<TransactionWithUser> all = repo.getAllTransactions().getValue();
        FilterState state = filterState.getValue();
        
        if (all == null || state == null) {
            filteredTransactions.postValue(new ArrayList<>());
            return;
        }

        // Run heavy filtering on background thread to keep UI smooth
        new Thread(() -> {
            List<TransactionWithUser> result = all.stream()
                .filter(tu -> {
                    // 1. Type Filter
                    if (state.type != null && !state.type.equalsIgnoreCase(tu.getTransaction().getType())) return false;
                    
                    // 2. User Filter
                    if (state.userId != null && !state.userId.equals(tu.getTransaction().getUserId())) return false;
                    
                    // 3. Category Filter
                    if (state.category != null) {
                        boolean matchMain = state.category.equalsIgnoreCase(tu.getTransaction().getCategory());
                        boolean matchSub = state.category.equalsIgnoreCase(tu.getTransaction().getSubCategory());
                        if (!matchMain && !matchSub) return false;
                    }
                    
                    // 4. Search Query (Smooth Search Logic)
                    if (!state.query.isEmpty()) {
                        String user = tu.getUsername() != null ? tu.getUsername().toLowerCase() : "";
                        String cat = tu.getTransaction().getCategory() != null ? tu.getTransaction().getCategory().toLowerCase() : "";
                        String note = tu.getTransaction().getNote() != null ? tu.getTransaction().getNote().toLowerCase() : "";
                        String amt = String.valueOf(tu.getTransaction().getAmount());
                        
                        return user.contains(state.query) || cat.contains(state.query) || note.contains(state.query) || amt.contains(state.query);
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
                
            filteredTransactions.postValue(result);
        }).start();
    }

    public void toggleLike(TransactionWithUser item) { repo.toggleLike(item, com.upreyvan.carti.utils.ReactionHelper.REAC_LIKE); }
    public void toggleReaction(TransactionWithUser item, String emoji) { repo.toggleLike(item, emoji); }
    public void deleteTransaction(com.upreyvan.carti.models.Transaction t, com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Object> cb) {
        repo.deleteItem(t.getType(), t.getId(), cb);
    }

    private record FilterState(String type, String userId, String category, String query) {}
}
