package com.upreyvan.carti.ui.allocate;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.NotificationRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.RecurringBudgetStats;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
        
        budgets.addSource(transRepo.getBudgetPlanLiveData(), items -> loadData());
        budgets.addSource(transRepo.getAllTransactions(), items -> loadData());
        
        categories.addSource(transRepo.getBudgetPlanLiveData(), items -> loadData());
        categories.addSource(transRepo.getAllTransactions(), items -> loadData());
        
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
            
            // Re-use repository logic to ensure consistency and correct month filtering
            List<BudgetCategoryItem> consolidated = transRepo.getBudgetPlan();
            if (consolidated == null) consolidated = new ArrayList<>();

            List<BudgetCategoryItem> budgetList = new ArrayList<>();
            for (BudgetCategoryItem item : consolidated) {
                if (item.getAmount() > 0) budgetList.add(item);
            }

            budgets.postValue(sortBudgetItems(budgetList));
            categories.postValue(sortBudgetItems(consolidated));
        });
    }

    public void deleteGoal(String goalId) {
        transRepo.deleteTransaction(goalId, null);
    }

    public void refresh() {
        transRepo.refreshTransactions();
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
