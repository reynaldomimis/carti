package com.upreyvan.carti.ui.allocate;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.local.db.dao.TransactionDao;
import com.upreyvan.carti.data.repository.NotificationRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.RecurringBudgetStats;
import com.upreyvan.carti.model.TransactionWithUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlanViewModel extends BaseViewModel {
    private final MutableLiveData<List<BudgetCategoryItem>> budgets = new MutableLiveData<>();
    private final MutableLiveData<List<BudgetCategoryItem>> categories = new MutableLiveData<>();
    private final MutableLiveData<RecurringBudgetStats> recurringStats = new MutableLiveData<>();
    
    private final BudgetManager budgetManager;
    private final TransactionRepository transRepo;
    private final NotificationRepository notifRepo;
    private final AppDatabase db;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlanViewModel(@NonNull Application application) {
        super(application);
        budgetManager = BudgetManager.getInstance(application);
        transRepo = TransactionRepository.getInstance(application);
        notifRepo = NotificationRepository.getInstance(application);
        db = AppDatabase.getInstance(application);
        pref = PreferenceManager.getInstance(application);
        
        budgetManager.getBudgetPlanLiveData().observeForever(observer);
        budgetManager.getRecurringStatsLiveData().observeForever(recurringStatsObserver);
    }

    private final androidx.lifecycle.Observer<List<BudgetCategoryItem>> observer = items -> loadData();
    private final androidx.lifecycle.Observer<RecurringBudgetStats> recurringStatsObserver = recurringStats::postValue;

    public LiveData<List<BudgetCategoryItem>> getBudgets() { return budgets; }
    public LiveData<List<BudgetCategoryItem>> getCategories() { return categories; }
    public LiveData<RecurringBudgetStats> getRecurringStats() { return recurringStats; }
    public LiveData<List<TransactionWithUser>> getGoals() { return transRepo.getGoals(); }
    public LiveData<List<Bill>> getBills() { return notifRepo.getBills(pref.getFamilyId()); }
    public LiveData<List<TransactionWithUser>> getDebts() { return transRepo.getTransactionsByType("DEBT"); }

    public void loadData() {
        executor.execute(() -> {
            budgetManager.processRecurringBudgets();
            String familyId = pref.getFamilyId();
            
            List<BudgetCategoryItem> plan = budgetManager.getBudgetPlan();
            List<BudgetCategoryItem> defaults = budgetManager.getDefaultCategories();
            List<TransactionDao.CategorySum> breakdown = db.transactionDao().getExpenseBreakdown(familyId);
            
            Map<String, Double> expenseMap = new HashMap<>();
            for (TransactionDao.CategorySum sum : breakdown) {
                if (sum.category != null) expenseMap.put(sum.category, sum.total);
            }

            List<BudgetCategoryItem> budgetList = new ArrayList<>();
            Map<String, BudgetCategoryItem> categoryMap = new HashMap<>();

            for (BudgetCategoryItem d : defaults) {
                if (d.getCategoryName() != null) categoryMap.put(d.getCategoryName().toLowerCase(Locale.ROOT), d);
            }

            for (BudgetCategoryItem item : plan) {
                String catName = (item.getCategoryName() != null) ? item.getCategoryName() : "General";
                Double spentVal = expenseMap.get(catName);
                double spent = (spentVal != null) ? spentVal : 0.0;

                BudgetCategoryItem updatedItem = new BudgetCategoryItem(
                        item.getCategoryName(), item.getIconRes(), item.getIconColor(), 
                        item.getBgColor(), item.getAmount(), item.getPercentage(), 
                        item.getParentCategory(), spent, item.isRecurring());
                
                if (item.getAmount() > 0) budgetList.add(updatedItem);
                categoryMap.put(catName.toLowerCase(Locale.ROOT), updatedItem);
            }

            budgets.postValue(sortBudgetItems(budgetList));
            categories.postValue(sortBudgetItems(new ArrayList<>(categoryMap.values())));
        });
    }

    public void deleteGoal(String goalId) {
        transRepo.deleteTransaction(goalId, null);
    }

    public void refresh() {
        transRepo.syncTransactionsIfNeeded();
        transRepo.refreshTransactions();
        loadData();
    }

    private List<BudgetCategoryItem> sortBudgetItems(List<BudgetCategoryItem> list) {
        if (list == null) return new ArrayList<>();
        List<BudgetCategoryItem> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> {
            boolean aOther = "others".equalsIgnoreCase(a.getCategoryName());
            boolean bOther = "others".equalsIgnoreCase(b.getCategoryName());
            if (aOther && bOther) return 0;
            if (aOther) return 1;
            if (bOther) return -1;
            return a.getCategoryName().compareToIgnoreCase(b.getCategoryName());
        });
        return sorted;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        budgetManager.getBudgetPlanLiveData().removeObserver(observer);
        budgetManager.getRecurringStatsLiveData().removeObserver(recurringStatsObserver);
        executor.shutdown();
    }
}
