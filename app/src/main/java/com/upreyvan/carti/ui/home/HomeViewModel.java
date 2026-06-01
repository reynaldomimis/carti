package com.upreyvan.carti.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.upreyvan.carti.base.BaseViewModel;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.NotificationRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class HomeViewModel extends BaseViewModel {

    private final TransactionRepository transRepo;
    private final NotificationRepository notifRepo;
    private final MemberRepository memberRepo;
    private final PreferenceManager pref;
    
    private final MediatorLiveData<DashboardState> dashboardState = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        transRepo = TransactionRepository.getInstance(application);
        notifRepo = NotificationRepository.getInstance(application);
        memberRepo = new MemberRepository(application);
        pref = new PreferenceManager(application);
        
        setupDashboardMediator();
    }

    public LiveData<DashboardState> getDashboardState() { return dashboardState; }
    public LiveData<List<TransactionWithUser>> getRecentTransactions() { return transRepo.getRecentTransactions(5); }
    public LiveData<List<Bill>> getDueBills() { return notifRepo.getBills(pref.getFamilyId()); }

    public void refreshData() {
        transRepo.syncTransactionsIfNeeded();
        memberRepo.refreshMembers();
    }

    private void setupDashboardMediator() {
        Calendar current = Calendar.getInstance();
        Calendar lastMonth = Calendar.getInstance();
        lastMonth.add(Calendar.MONTH, -1);

        long curStart = Utils.getMonthStartMillis(current);
        long curEnd = Utils.getMonthEndMillis(current);
        long lstStart = Utils.getMonthStartMillis(lastMonth);
        long lstEnd = Utils.getMonthEndMillis(lastMonth);

        LiveData<Double> curIncome = transRepo.getTotalIncomeInRange(curStart, curEnd);
        LiveData<Double> curExpense = transRepo.getTotalExpenseInRange(curStart, curEnd);
        LiveData<Double> lstIncome = transRepo.getTotalIncomeInRange(lstStart, lstEnd);
        LiveData<Double> lstExpense = transRepo.getTotalExpenseInRange(lstStart, lstEnd);

        dashboardState.addSource(curIncome, income -> updateDashboard(income, curExpense.getValue(), lstIncome.getValue(), lstExpense.getValue()));
        dashboardState.addSource(curExpense, expense -> updateDashboard(curIncome.getValue(), expense, lstIncome.getValue(), lstExpense.getValue()));
        dashboardState.addSource(lstIncome, income -> updateDashboard(curIncome.getValue(), curExpense.getValue(), income, lstExpense.getValue()));
        dashboardState.addSource(lstExpense, expense -> updateDashboard(curIncome.getValue(), curExpense.getValue(), lstIncome.getValue(), expense));
    }

    private void updateDashboard(Double curInc, Double curExp, Double lstInc, Double lstExp) {
        double income = Objects.requireNonNullElse(curInc, 0.0);
        double expense = Objects.requireNonNullElse(curExp, 0.0);
        double lastInc = Objects.requireNonNullElse(lstInc, 0.0);
        double lastExp = Objects.requireNonNullElse(lstExp, 0.0);
        
        dashboardState.setValue(new DashboardState(
                pref.getBalance(),
                income,
                expense,
                income - expense,
                calculateTrend(income, lastInc),
                calculateTrend(expense, lastExp),
                calculateTrend(income - expense, lastInc - lastExp)
        ));
    }

    private double calculateTrend(double current, double previous) {
        if (previous == 0) return 0;
        return ((current - previous) / previous) * 100;
    }

    public record DashboardState(
        double balance,
        double monthlyIncome,
        double monthlyExpense,
        double monthlySavings,
        double incomeTrend,
        double expenseTrend,
        double savingsTrend
    ) {}
}
