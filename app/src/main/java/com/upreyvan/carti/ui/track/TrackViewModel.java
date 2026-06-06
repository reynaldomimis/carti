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
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.data.local.db.dao.TransactionDao.CategorySum;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.TrackCategory;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.ValueHelper;
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
    public List<BudgetCategoryItem> getAllocations() { return isExpanded ? currentAllocations : currentAllocations.subList(0, Math.min(currentAllocations.size(), 3)); }
    public void toggleExpansion() { isExpanded = !isExpanded; rebuild(); }

    private void setupDataStream() {
        dataTrigger.addSource(repo.getTransactionsByMonth(month, year), v -> { currentMonthTrans = v; rebuild(); });
        dataTrigger.addSource(repo.getGoals(), v -> { currentGoals = v; rebuild(); });
        dataTrigger.addSource(BudgetManager.getInstance(getApplication()).getBudgetPlanLiveData(), v -> { rebuild(); });
        dataTrigger.addSource(repo.getAllocationsByMonth(com.upreyvan.carti.util.Utils.formatMonthQuery(java.util.Calendar.getInstance())), v -> { currentDbAllocations = v; rebuild(); });
    }

    private void rebuild() {
        executor.execute(() -> {
            List<BaseMultiItem> items = new ArrayList<>();
            double inc = 0, exp = 0;
            Map<String, Double> catTotals = new HashMap<>();
            List<TransactionWithUser> filteredExp = new ArrayList<>();
            
            if (currentMonthTrans != null) {
                for (TransactionWithUser t : currentMonthTrans) {
                    if ("INCOME".equals(t.getTransaction().getType())) inc += t.getTransaction().getAmount();
                    else if ("EXPENSE".equals(t.getTransaction().getType())) {
                        exp += t.getTransaction().getAmount();
                        String normCat = normalizeCategory(t.getTransaction().getCategory());
                        catTotals.merge(normCat, t.getTransaction().getAmount(), Double::sum);
                        filteredExp.add(t);
                    }
                }
            }

            double saved = 0, target = 0;
            if (currentGoals != null) {
                for (TransactionWithUser g : currentGoals) {
                    saved += g.getTransaction().getAmount();
                    target += g.getTransaction().getTargetAmount();
                }
            }
            items.add(new TrackListItem.SummaryItem(inc - exp, inc, exp, saved, target, target > 0 ? (int)((saved/target)*100) : 0, this instanceof TrackListItem.OnTrackInteractionListener ? (TrackListItem.OnTrackInteractionListener) this : null));

            List<TrackListItem.PieEntryData> pie = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            List<TrackCategory> legend = new ArrayList<>();
            int[] colorRes = { R.color.status_red, R.color.dash_orange, R.color.icon_electricity, R.color.icon_water, R.color.carti_primary_blue, R.color.purple };
            int cIdx = 0;
            for (Map.Entry<String, Double> e : catTotals.entrySet()) {
                float p = exp > 0 ? (float)(e.getValue()/exp*100) : 0;
                pie.add(new TrackListItem.PieEntryData(e.getValue().floatValue(), e.getKey()));
                int color = getApplication().getColor(colorRes[cIdx % colorRes.length]);
                colors.add(color);
                legend.add(new TrackCategory(e.getKey(), e.getValue(), p, color));
                cIdx++;
            }
            if (!pie.isEmpty()) items.add(new TrackListItem.ChartItem(pie, colors, legend, exp));
            items.add(new TrackListItem.ComparisonItem(inc, exp));

            List<CategorySum> expenseBreakdown = new ArrayList<>();
            for (Map.Entry<String, Double> entry : catTotals.entrySet()) {
                CategorySum cs = new CategorySum();
                cs.category = entry.getKey();
                cs.total = entry.getValue();
                expenseBreakdown.add(cs);
            }

            List<BudgetCategoryItem> consolidated = BudgetManager.getInstance(getApplication()).getConsolidatedBudgets(currentDbAllocations, expenseBreakdown);

            // Strictly synchronize: Only show categories that have an active budget allocation (Amount > 0)
            List<BudgetCategoryItem> nextAllocations = new ArrayList<>();
            double totalPlanAmount = 0;
            for (BudgetCategoryItem bi : consolidated) {
                if (bi.getAmount() > 0) {
                    nextAllocations.add(bi);
                    totalPlanAmount += bi.getAmount();
                }
            }

            nextAllocations.sort((a, b) -> {
                boolean aOther = "others".equalsIgnoreCase(a.getCategoryName());
                boolean bOther = "others".equalsIgnoreCase(b.getCategoryName());
                if (aOther && bOther) return 0;
                if (aOther) return 1;
                if (bOther) return -1;
                return a.getCategoryName().compareToIgnoreCase(b.getCategoryName());
            });

            if (!nextAllocations.isEmpty()) {
                items.add(new TrackListItem.AllocationHeaderItem(totalPlanAmount, isExpanded, nextAllocations.size(), null, null));
            }

            items.add(new TrackListItem.SectionHeaderItem(getApplication().getString(R.string.recent_expenses), getApplication().getString(R.string.see_all), null));
            filteredExp.sort((a,b) -> Long.compare(b.getTransaction().getTimestampMillis(), a.getTransaction().getTimestampMillis()));
            for (int i=0; i<Math.min(filteredExp.size(), 5); i++) items.add(new TrackListItem.TransactionItem(filteredExp.get(i), null));

            mainHandler.post(() -> {
                currentAllocations = nextAllocations;
                uiState.setValue(items);
            });
        });
    }

    private String normalizeCategory(String cat) {
        if (cat == null || cat.trim().isEmpty()) return "Others";
        String s = cat.trim().toLowerCase();
        if (s.contains("food") || s.contains("fooo")) return "Food";
        if (s.contains("tran") || s.contains("fare") || s.contains("grab")) return "Transportation";
        if (s.contains("bill") || s.contains("util")) return "Bills";
        if (s.contains("shop") || s.contains("buy")) return "Shopping";
        if (s.contains("health") || s.contains("med")) return "Health";
        if (s.contains("school") || s.contains("educ")) return "Education";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public void sync() { repo.syncTransactionsIfNeeded(); }

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
