package com.upreyvan.carti.ui.track;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseMultiItem;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.BudgetCategoryItem;
import com.upreyvan.carti.models.TrackCategory;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackViewModel extends BaseViewModel {
    private final TransactionRepository repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MediatorLiveData<List<BaseMultiItem>> uiState = new MediatorLiveData<>();
    private final MediatorLiveData<Object> dataTrigger = new MediatorLiveData<>();

    private List<BudgetCategoryItem> currentAllocations = new ArrayList<>();
    private boolean isExpanded = false;

    public TrackViewModel(@NonNull Application application) {
        super(application);
        repo = TransactionRepository.getInstance(application);
        setupDataStream();
        uiState.addSource(dataTrigger, v -> {});
    }

    public LiveData<List<BaseMultiItem>> getUiState() { return uiState; }
    public List<BudgetCategoryItem> getAllocations() { return currentAllocations; }
    public List<BudgetCategoryItem> getDisplayAllocations() { return isExpanded ? currentAllocations : currentAllocations.subList(0, Math.min(currentAllocations.size(), 5)); }
    public void toggleExpansion() { isExpanded = !isExpanded; rebuild(); }

    private void setupDataStream() {
        dataTrigger.addSource(repo.getFinancialSummary(), v -> rebuild());
        dataTrigger.addSource(repo.getAllTransactions(), v -> rebuild());
    }

    private void rebuild() {
        executor.execute(() -> {
            com.upreyvan.carti.models.FinancialSummary fs = repo.getFinancialSummary().getValue();
            if (fs == null) return;

            List<BaseMultiItem> items = new ArrayList<>();

            double balance = fs.monthlyBalance();
            double totalBudget = fs.monthlyBudget();
            double totalExpense = fs.monthlyExpense();

            // Recent Expenses from main transaction stream (limit to 5)
            List<TransactionWithUser> allTrans = repo.getAllTransactions().getValue();
            final List<TransactionWithUser> recentExpenses;
            if (allTrans != null) {
                recentExpenses = allTrans.stream()
                        .filter(tu -> "EXPENSE".equalsIgnoreCase(tu.getTransaction().getType()))
                        .limit(5)
                        .collect(java.util.stream.Collectors.toList());
            } else {
                recentExpenses = new ArrayList<>();
            }

            // Centralized Budget/Allocation logic
            this.currentAllocations = repo.getBudgetPlan();

            mainHandler.post(() -> {
                items.add(new TrackListItem.SummaryItem(
                    fs.monthlyBalance(), 
                    fs.monthlyBudget(), 
                    fs.monthlyExpense(), 
                    fs.totalAccumulatedSavings(),
                    0,
                    fs.savingsProgress(), 
                    this instanceof TrackListItem.OnTrackInteractionListener ? (TrackListItem.OnTrackInteractionListener) this : null
                ));

                // Chart Item from fs.categoryBreakdown()
                List<TrackListItem.PieEntryData> pie = new ArrayList<>();
                List<Integer> colors = new ArrayList<>();
                List<TrackCategory> legend = new ArrayList<>();
                int[] colorRes = { R.color.status_red, R.color.dash_orange, R.color.icon_electricity, R.color.icon_water, R.color.carti_primary_blue, R.color.purple };
                int cIdx = 0;
                
                for (com.upreyvan.carti.models.FinancialSummary.CategoryTotal ct : fs.categoryBreakdown()) {
                    pie.add(new TrackListItem.PieEntryData((float)ct.amount(), ct.category()));
                    int color = getApplication().getColor(colorRes[cIdx % colorRes.length]);
                    colors.add(color);
                    legend.add(new TrackCategory(ct.category(), ct.amount(), (float)ct.percentage(), color));
                    cIdx++;
                }
                if (!pie.isEmpty()) items.add(new TrackListItem.ChartItem(pie, colors, legend, totalExpense));

                // Comparison Item
                items.add(new TrackListItem.ComparisonItem(totalBudget, totalExpense));

                // Allocation Card
                if (!currentAllocations.isEmpty()) {
                    items.add(new TrackListItem.AllocationHeaderItem(totalBudget, totalExpense, isExpanded, getDisplayAllocations(), this instanceof TrackListItem.OnTrackInteractionListener ? (TrackListItem.OnTrackInteractionListener) this : null, null));
                }

                // Recent Expenses Section
                items.add(new TrackListItem.SectionHeaderItem(getApplication().getString(R.string.recent_expenses), getApplication().getString(R.string.see_all), null));
                for (TransactionWithUser tu : recentExpenses) items.add(new TrackListItem.TransactionItem(tu, null));

                uiState.setValue(items);
            });
        });
    }

    public void sync() { repo.refreshTransactions(); }

    public void toggleLike(TransactionWithUser item) { repo.toggleLike(item, "👍"); }

    public void toggleReaction(TransactionWithUser item, String emoji) { repo.toggleLike(item, emoji); }

    public void deleteTransaction(TransactionWithUser item) {
        Transaction transaction = item.getTransaction();
        repo.deleteItem(transaction.getType(), transaction.getId(), null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
