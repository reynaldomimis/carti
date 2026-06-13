package com.upreyvan.carti.ui.allocate;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.NotificationRepository;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.BudgetCategoryItem;
import com.upreyvan.carti.models.RecurringBudgetStats;
import com.upreyvan.carti.models.TransactionType;
import com.upreyvan.carti.models.TransactionWithUser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlanViewModel extends BaseViewModel {
    private final MediatorLiveData<List<BudgetCategoryItem>> budgets = new MediatorLiveData<>();
    private final MediatorLiveData<List<BudgetCategoryItem>> categories = new MediatorLiveData<>();
    private final MediatorLiveData<RecurringBudgetStats> recurringStats = new MediatorLiveData<>();
    
    private final TransactionRepository transRepo;
    private final NotificationRepository notifRepo;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlanViewModel(@NonNull Application application) {
        super(application);
        transRepo = TransactionRepository.getInstance(application);
        notifRepo = NotificationRepository.getInstance(application);
        pref = PreferenceManager.getInstance(application);
        
        budgets.addSource(transRepo.getFinancialSummary(), fs -> loadData());
        budgets.addSource(transRepo.getAllTransactions(), items -> loadData());
        
        categories.addSource(transRepo.getBudgetPlanLiveData(), items -> loadData());
        
        recurringStats.addSource(transRepo.getRecurringStatsLiveData(), recurringStats::postValue);
    }

    public LiveData<List<BudgetCategoryItem>> getBudgets() { return budgets; }
    public LiveData<List<BudgetCategoryItem>> getCategories() { return categories; }
    public LiveData<RecurringBudgetStats> getRecurringStats() { return recurringStats; }
    public LiveData<List<TransactionWithUser>> getGoals() { return transRepo.getGoals(); }
    public LiveData<List<Bill>> getBills() { return notifRepo.getBills(pref.getFamilyId()); }
    public LiveData<List<TransactionWithUser>> getDebts() { return transRepo.getTransactionsByType("DEBT"); }

    public void loadData() {
        executor.execute(() -> {
            transRepo.processRecurringBudgets();
            
            // 1. Load Budgets from Centralized Logic
            List<BudgetCategoryItem> consolidated = transRepo.getBudgetPlan();
            List<BudgetCategoryItem> budgetList = new ArrayList<>();
            if (consolidated != null) {
                for (BudgetCategoryItem item : consolidated) {
                    if (item.getAmount() > 0) budgetList.add(item);
                }
            }
            budgets.postValue(sortBudgetItems(budgetList));

            // 2. Load Base Categories
            List<com.upreyvan.carti.models.Category> localCats = com.upreyvan.carti.managers.CategoryManager.getInstance(getApplication()).getCategories();
            List<BudgetCategoryItem> categoryList = new ArrayList<>();
            for (com.upreyvan.carti.models.Category c : localCats) {
                categoryList.add(new BudgetCategoryItem(
                        c.getName(), c.getIconRes(), c.getIconColor(), c.getBackgroundColor(),
                        0, 0, c.getParentCategory(), 0, false
                ));
            }
            categories.postValue(sortBudgetItems(categoryList));
        });
    }

    public void deleteGoal(String goalId) { transRepo.deleteItem(TransactionType.GOAL, goalId, null); }
    public void refresh() { 
        transRepo.refreshTransactions(); 
        com.upreyvan.carti.managers.CategoryManager.getInstance(getApplication()).refreshRemoteCategories(pref.getFamilyId());
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
        executor.shutdown();
    }
}
