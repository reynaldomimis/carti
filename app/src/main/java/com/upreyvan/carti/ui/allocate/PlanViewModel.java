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
import com.upreyvan.carti.model.BudgetCategoryItem;

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
    private final BudgetManager budgetManager;
    private final AppDatabase db;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlanViewModel(@NonNull Application application) {
        super(application);
        budgetManager = BudgetManager.getInstance(application);
        db = AppDatabase.getInstance(application);
        pref = PreferenceManager.getInstance(application);
        
        // Listen to local budget changes and refresh automatically
        budgetManager.getBudgetPlanLiveData().observeForever(observer);
    }

    private final androidx.lifecycle.Observer<List<BudgetCategoryItem>> observer = items -> loadData();

    public LiveData<List<BudgetCategoryItem>> getBudgets() { return budgets; }
    public LiveData<List<BudgetCategoryItem>> getCategories() { return categories; }

    public void loadData() {
        executor.execute(() -> {
            List<BudgetCategoryItem> plan = budgetManager.getBudgetPlan();
            List<BudgetCategoryItem> defaults = budgetManager.getDefaultCategories();
            String familyId = pref.getFamilyId();

            List<TransactionDao.CategorySum> breakdown = db.transactionDao().getExpenseBreakdown(familyId);
            Map<String, Double> expenseMap = new HashMap<>();
            for (TransactionDao.CategorySum sum : breakdown) {
                if (sum.category != null) {
                    expenseMap.put(sum.category, sum.total);
                }
            }

            List<BudgetCategoryItem> budgetList = new ArrayList<>();
            Map<String, BudgetCategoryItem> categoryMap = new HashMap<>();


            for (BudgetCategoryItem d : defaults) {
                String name = d.getCategoryName();
                if (name != null) {
                    categoryMap.put(name.toLowerCase(Locale.ROOT), d);
                }
            }

            for (BudgetCategoryItem item : plan) {
                String catName = (item.getCategoryName() != null) ? item.getCategoryName() : "General";
                
                Double spentVal = expenseMap.get(catName);
                double spent = (spentVal != null) ? spentVal : 0.0;

                BudgetCategoryItem updatedItem = new BudgetCategoryItem(
                        item.getCategoryName(), item.getIconRes(), item.getIconColor(), 
                        item.getBgColor(), item.getAmount(), item.getPercentage(), 
                        item.getParentCategory(), spent);
                
                if (item.getAmount() > 0) {
                    budgetList.add(updatedItem);
                }
                categoryMap.put(catName.toLowerCase(Locale.ROOT), updatedItem);
            }

            budgets.postValue(budgetList);
            categories.postValue(new ArrayList<>(categoryMap.values()));
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        budgetManager.getBudgetPlanLiveData().removeObserver(observer);
        executor.shutdown();
    }
}
