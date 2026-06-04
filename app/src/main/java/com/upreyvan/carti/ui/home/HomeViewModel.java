package com.upreyvan.carti.ui.home;

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
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.NotificationRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends BaseViewModel {

    private final TransactionRepository transRepo;
    private final NotificationRepository notifRepo;
    private final MemberRepository memberRepo;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private final MediatorLiveData<List<BaseMultiItem>> uiState = new MediatorLiveData<>();
    private final MediatorLiveData<Object> dataTrigger = new MediatorLiveData<>();

    private DashboardState currentDash;
    private List<Bill> currentBills;
    private List<TransactionWithUser> currentTransactions;
    private List<BudgetCategoryItem> currentPlan;

    private Double curInc = 0.0, curExp = 0.0, lstInc = 0.0, lstExp = 0.0;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        transRepo = TransactionRepository.getInstance(application);
        notifRepo = NotificationRepository.getInstance(application);
        memberRepo = new MemberRepository(application);
        pref = PreferenceManager.getInstance(application);
        setupDataStream();
        uiState.addSource(dataTrigger, v -> {});
    }

    public LiveData<List<BaseMultiItem>> getUiState() { return uiState; }

    public void refreshData() {
        transRepo.syncTransactionsIfNeeded();
        memberRepo.refreshMembers();
    }

    private void setupDataStream() {
        Calendar current = Calendar.getInstance();
        Calendar lastMonth = Calendar.getInstance();
        lastMonth.add(Calendar.MONTH, -1);

        long curStart = Utils.getMonthStartMillis(current);
        long curEnd = Utils.getMonthEndMillis(current);
        long lstStart = Utils.getMonthStartMillis(lastMonth);
        long lstEnd = Utils.getMonthEndMillis(lastMonth);

        dataTrigger.addSource(transRepo.getTotalIncomeInRange(curStart, curEnd), v -> { curInc = v; updateDash(); rebuild(); });
        dataTrigger.addSource(transRepo.getTotalExpenseInRange(curStart, curEnd), v -> { curExp = v; updateDash(); rebuild(); });
        dataTrigger.addSource(transRepo.getTotalIncomeInRange(lstStart, lstEnd), v -> { lstInc = v; updateDash(); rebuild(); });
        dataTrigger.addSource(transRepo.getTotalExpenseInRange(lstStart, lstEnd), v -> { lstExp = v; updateDash(); rebuild(); });
        dataTrigger.addSource(transRepo.getRecentTransactions(5), v -> { this.currentTransactions = v; rebuild(); });
        dataTrigger.addSource(notifRepo.getBills(pref.getFamilyId()), v -> { this.currentBills = v; rebuild(); });
        dataTrigger.addSource(BudgetManager.getInstance(getApplication()).getBudgetPlanLiveData(), v -> { this.currentPlan = v; rebuild(); });
    }

    private void updateDash() {
        double inc = curInc != null ? curInc : 0.0;
        double exp = curExp != null ? curExp : 0.0;
        double lInc = lstInc != null ? lstInc : 0.0;
        double lExp = lstExp != null ? lstExp : 0.0;
        this.currentDash = new DashboardState(pref.getBalance(), inc, exp, inc - exp, calculateTrend(inc, lInc), calculateTrend(exp, lExp), calculateTrend(inc - exp, lInc - lExp));
    }

    private void rebuild() {
        executor.execute(() -> {
            List<BaseMultiItem> items = new ArrayList<>();
            if (currentDash != null) items.add(new HomeListItem.DashboardItem(currentDash));
            items.add(new HomeListItem.AIInsightItem("Analyzing budget... 🙌"));
            if (currentPlan == null || currentPlan.isEmpty()) items.add(new HomeListItem.BudgetPromptItem(null));
            if (currentBills != null && !currentBills.isEmpty()) {
                items.add(new HomeListItem.SectionHeaderItem(getApplication().getString(R.string.due_bills_header), null, false, null, null));
                items.add(new HomeListItem.BillContainerItem(new ArrayList<>(currentBills), null, null));
            }
            items.add(new HomeListItem.SectionHeaderItem(getApplication().getString(R.string.quick_actions_title), null, false, null, null));
            List<QuickLogItem> act = new ArrayList<>();
            act.add(new QuickLogItem(getApplication().getString(R.string.add_options_expense), R.drawable.ic_add, R.color.status_red_tonal, R.color.status_red));
            act.add(new QuickLogItem(getApplication().getString(R.string.action_add_income), R.drawable.ic_arrow_up, R.color.dash_green_alpha, R.color.dash_green));
            act.add(new QuickLogItem(getApplication().getString(R.string.action_family_chat), R.drawable.ic_sync, R.color.log_fare, R.color.carti_primary_blue));
            act.add(new QuickLogItem(getApplication().getString(R.string.action_manage_goals), R.drawable.ic_trophy, R.color.mint_green_alpha, R.color.mint_green));
            items.add(new HomeListItem.QuickActionsItem(act, null, null));
            if (currentPlan != null && !currentPlan.isEmpty()) {
                items.add(new HomeListItem.SectionHeaderItem(getApplication().getString(R.string.quick_log_title), getApplication().getString(R.string.quick_log_subtitle), false, null, null));
                List<QuickLogItem> logs = new ArrayList<>();
                for (BudgetCategoryItem p : currentPlan) logs.add(new QuickLogItem(p.getCategoryName(), p.getIconRes(), p.getBgColor(), p.getIconColor()));
                items.add(new HomeListItem.QuickLogItemContainer(logs, null, null));
            }
            items.add(new HomeListItem.SectionHeaderItem(getApplication().getString(R.string.recent_activity), null, currentTransactions != null && !currentTransactions.isEmpty(), "View All", null));
            if (currentTransactions == null || currentTransactions.isEmpty()) items.add(new HomeListItem.EmptyStateItem(getApplication().getString(R.string.no_transactions_yet)));
            else for (TransactionWithUser t : currentTransactions) items.add(new HomeListItem.TransactionItem(t, null));
            mainHandler.post(() -> uiState.setValue(items));
        });
    }

    private double calculateTrend(double c, double p) { return p == 0 ? 0 : ((c - p) / p) * 100; }
    public record DashboardState(double balance, double monthlyIncome, double monthlyExpense, double monthlySavings, double incomeTrend, double expenseTrend, double savingsTrend) {}
}
