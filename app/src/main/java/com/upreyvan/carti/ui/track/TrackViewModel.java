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
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.TrackCategory;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackViewModel extends BaseViewModel {
    private final TransactionRepository repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MediatorLiveData<List<BaseMultiItem>> uiState = new MediatorLiveData<>();
    private final MediatorLiveData<Object> dataTrigger = new MediatorLiveData<>();

    private List<TransactionWithUser> currentMonthTrans;
    private List<TransactionWithUser> currentGoals;
    private List<TransactionWithUser> currentDbAllocations;
    private List<BudgetCategoryItem> currentAllocations = new ArrayList<>();
    private final int month, year;
    private boolean isExpanded = false;

    public TrackViewModel(@NonNull Application application) {
        super(application);
        repo = TransactionRepository.getInstance(application);
        Calendar cal = Calendar.getInstance();
        month = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);
        setupDataStream();
        uiState.addSource(dataTrigger, v -> {});
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public LiveData<List<BaseMultiItem>> getUiState() { return uiState; }
    public List<BudgetCategoryItem> getAllocations() { return currentAllocations; }
    public List<BudgetCategoryItem> getDisplayAllocations() { return isExpanded ? currentAllocations : currentAllocations.subList(0, Math.min(currentAllocations.size(), 5)); }
    public void toggleExpansion() { isExpanded = !isExpanded; rebuild(); }

    private void setupDataStream() {
        dataTrigger.addSource(repo.getTransactionsByMonth(month, year), v -> { currentMonthTrans = v; rebuild(); });
        dataTrigger.addSource(repo.getGoals(), v -> { currentGoals = v; rebuild(); });
        dataTrigger.addSource(repo.getAllocationsByMonth(com.upreyvan.carti.util.Utils.formatMonthQuery(java.util.Calendar.getInstance())), v -> { currentDbAllocations = v; rebuild(); });
    }

    private void rebuild() {
        executor.execute(() -> {
            List<BaseMultiItem> items = new ArrayList<>();
            double exp = 0;
            Map<String, Double> topLevelTotals = new HashMap<>();
            Map<String, Double> breakdownMap = new HashMap<>();
            List<TransactionWithUser> filteredExp = new ArrayList<>();
            
            if (currentMonthTrans != null) {
                for (TransactionWithUser t : currentMonthTrans) {
                    Transaction trans = t.getTransaction();
                    if ("EXPENSE".equals(trans.getType())) {
                        exp += trans.getAmount();
                        String cat = normalizeCategory(trans.getCategory());
                        topLevelTotals.merge(cat, trans.getAmount(), Double::sum);
                        breakdownMap.merge(cat, trans.getAmount(), Double::sum);
                        if (trans.getSubCategory() != null) {
                            breakdownMap.merge(normalizeCategory(trans.getSubCategory()), trans.getAmount(), Double::sum);
                        }
                        filteredExp.add(t);
                    }
                }
            }

            List<TransactionRepository.CategorySum> expenseBreakdown = new ArrayList<>();
            for (Map.Entry<String, Double> entry : breakdownMap.entrySet()) {
                expenseBreakdown.add(new TransactionRepository.CategorySum(entry.getKey(), entry.getValue()));
            }

            List<BudgetCategoryItem> consolidated = repo.getConsolidatedBudgets(currentDbAllocations, expenseBreakdown);
            
            double totalBudget = 0;
            List<BudgetCategoryItem> allValidAllocations = new ArrayList<>();
            for (BudgetCategoryItem bi : consolidated) {
                if (bi.getAmount() > 0) {
                    allValidAllocations.add(bi);
                    totalBudget += bi.getAmount();
                }
            }

            double goalSaved = 0, goalTarget = 0;
            if (currentGoals != null) {
                for (TransactionWithUser g : currentGoals) {
                    goalSaved += g.getTransaction().getAmount();
                    goalTarget += g.getTransaction().getTargetAmount();
                }
            }

            final double finalExp = exp;
            final double finalBudget = totalBudget;
            final double finalSaved = goalSaved;
            final double finalTarget = goalTarget;
            final List<TransactionWithUser> finalFiltered = filteredExp;
            final List<BudgetCategoryItem> finalValidAllocations = allValidAllocations;

            mainHandler.post(() -> {
                currentAllocations = finalValidAllocations;
                
                // Summary Item (Budget - Expense as balance, Total Budget, Total Expense, Goal Progress)
                items.add(new TrackListItem.SummaryItem(finalBudget - finalExp, finalBudget, finalExp, finalSaved, finalTarget, finalTarget > 0 ? (int)((finalSaved/finalTarget)*100) : 0, this instanceof TrackListItem.OnTrackInteractionListener ? (TrackListItem.OnTrackInteractionListener) this : null));

                // Chart Item
                List<TrackListItem.PieEntryData> pie = new ArrayList<>();
                List<Integer> colors = new ArrayList<>();
                List<TrackCategory> legend = new ArrayList<>();
                int[] colorRes = { R.color.status_red, R.color.dash_orange, R.color.icon_electricity, R.color.icon_water, R.color.carti_primary_blue, R.color.purple };
                int cIdx = 0;
                for (Map.Entry<String, Double> e : topLevelTotals.entrySet()) {
                    float p = finalExp > 0 ? (float)(e.getValue()/finalExp*100) : 0;
                    pie.add(new TrackListItem.PieEntryData(e.getValue().floatValue(), e.getKey()));
                    colors.add(getApplication().getColor(colorRes[cIdx % colorRes.length]));
                    legend.add(new TrackCategory(e.getKey(), e.getValue(), p, colors.get(colors.size()-1)));
                    cIdx++;
                }
                if (!pie.isEmpty()) items.add(new TrackListItem.ChartItem(pie, colors, legend, finalExp));

                // Comparison Item
                items.add(new TrackListItem.ComparisonItem(finalBudget, finalExp));

                // Allocation Card
                if (!currentAllocations.isEmpty()) {
                    items.add(new TrackListItem.AllocationHeaderItem(finalBudget, finalExp, isExpanded, getDisplayAllocations(), this instanceof TrackListItem.OnTrackInteractionListener ? (TrackListItem.OnTrackInteractionListener) this : null, null));
                }

                // Recent Expenses
                items.add(new TrackListItem.SectionHeaderItem(getApplication().getString(R.string.recent_expenses), getApplication().getString(R.string.see_all), null));
                finalFiltered.sort((a,b) -> Long.compare(b.getTransaction().getTimestampMillis(), a.getTransaction().getTimestampMillis()));
                for (int i=0; i<Math.min(finalFiltered.size(), 5); i++) items.add(new TrackListItem.TransactionItem(finalFiltered.get(i), null));

                uiState.setValue(items);
            });
        });
    }

    private String normalizeCategory(String cat) {
        if (cat == null || cat.trim().isEmpty()) return "Others";
        return cat.trim();
    }

    public void sync() { repo.refreshTransactions(); }

    public void toggleLike(TransactionWithUser item) {
        repo.toggleLike(item, "👍");
    }

    public void toggleReaction(TransactionWithUser item, String emoji) {
        repo.toggleLike(item, emoji);
    }

    public void deleteTransaction(TransactionWithUser item) {
        repo.deleteTransaction(item.getTransaction().getId(), null);
    }
}
