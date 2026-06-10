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
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.AiRepository;
import com.upreyvan.carti.repository.MemberRepository;
import com.upreyvan.carti.repository.NotificationRepository;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.BudgetCategoryItem;
import com.upreyvan.carti.models.QuickLogItem;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends BaseViewModel {

    private final TransactionRepository transRepo;
    private final NotificationRepository notifRepo;
    private final MemberRepository memberRepo;
    private final AiRepository aiRepo;
    private final PreferenceManager pref;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private final MediatorLiveData<List<BaseMultiItem>> uiState = new MediatorLiveData<>();
    private final MediatorLiveData<Object> dataTrigger = new MediatorLiveData<>();

    private DashboardState currentDash;
    private List<Bill> currentBills;
    private List<TransactionWithUser> currentTransactions;
    private List<BudgetCategoryItem> currentPlan;
    private List<TransactionWithUser> allTransactionsList;
    private String aiInsight;
    private boolean isAiFetching = false;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        transRepo = TransactionRepository.getInstance(application);
        notifRepo = NotificationRepository.getInstance(application);
        memberRepo = MemberRepository.getInstance(application);
        aiRepo = AiRepository.getInstance(application);
        pref = PreferenceManager.getInstance(application);
        setupDataStream();
        uiState.addSource(dataTrigger, v -> {});
    }

    public LiveData<List<BaseMultiItem>> getUiState() { return uiState; }

    public void refreshData() {
        transRepo.refreshTransactions();
        memberRepo.refreshMembers();
        aiInsight = null;
        fetchAiInsight();
    }

    private void setupDataStream() {
        dataTrigger.addSource(transRepo.getAllTransactions(), v -> { allTransactionsList = v; rebuild(false); });
        dataTrigger.addSource(notifRepo.getBills(pref.getFamilyId()), v -> { this.currentBills = v; rebuild(false); });
        dataTrigger.addSource(transRepo.getBudgetPlanLiveData(), v -> { this.currentPlan = v; rebuild(false); });
        dataTrigger.addSource(transRepo.getSyncingStatus(), v -> rebuild(false));
    }

    private void checkAiTrigger() {
        if (aiInsight == null && !isAiFetching) {
            String cached = pref.getAiInsightsCache();
            long timestamp = pref.getAiInsightsTimestamp();
            
            boolean isBoring = cached.toLowerCase().contains("no urgent");
            
            if (!cached.isEmpty() && timestamp >= Utils.getStartOfDayMillis() && !isBoring) {
                aiInsight = cached;
                rebuild(false);
            } else {
                fetchAiInsight();
            }
        }
    }

    private void fetchAiInsight() {
        if (currentDash == null || isAiFetching) return;
        
        isAiFetching = true;
        rebuild(false); 
        
        StringBuilder ctxBuilder = new StringBuilder();
        ctxBuilder.append(String.format("CURRENCY: Philippine Peso (PHP, ₱). Balance: ₱%.2f. Monthly Income: ₱%.2f. Monthly Expense: ₱%.2f.\n",
                currentDash.balance, currentDash.monthlyIncome, currentDash.monthlyExpense));
        
        if (currentBills != null && !currentBills.isEmpty()) {
            ctxBuilder.append("Upcoming/Overdue Bills:\n");
            for (Bill b : currentBills) {
                ctxBuilder.append(String.format("- %s (₱%.2f) Due: %s\n", b.getName(), b.getAmount(), b.getDate()));
            }
        }
        
        if (currentPlan != null && !currentPlan.isEmpty()) {
            ctxBuilder.append("Budget Progress:\n");
            for (BudgetCategoryItem p : currentPlan) {
                double usage = p.getAmount() > 0 ? (p.getCurrentSpent() / p.getAmount()) * 100 : 0;
                ctxBuilder.append(String.format("- %s: %.0f%% used.\n", p.getCategoryName(), usage));
            }
        }

        aiRepo.getInsights(ctxBuilder.toString(), new AiRepository.AiCallback() {
            @Override public void onSuccess(String response) {
                isAiFetching = false;
                aiInsight = response;
                pref.setAiInsightsCache(response);
                rebuild(false);
            }
            @Override public void onError(Throwable t) {
                isAiFetching = false;
                if (aiInsight == null) {
                    aiInsight = "[{\"type\": \"INFO\", \"message\": \"Kakatapos ko lang mag-analyze, check mo dashboard natin! 🙌\"}]";
                    rebuild(false);
                }
            }
            @Override public void onActionDetected(org.json.JSONObject action) {
                isAiFetching = false;
            }
        });
    }

    private void rebuild(boolean instant) {
        Runnable task = () -> {
            if (allTransactionsList != null) {
                Calendar cal = Calendar.getInstance();
                long curStart = Utils.getMonthStartMillis(cal);
                long curEnd = Utils.getMonthEndMillis(cal);
                long todayStart = Utils.getStartOfDayMillis();
                String curMonthQuery = Utils.formatMonthQuery(cal);
                
                cal.add(Calendar.MONTH, -1);
                long lstStart = Utils.getMonthStartMillis(cal);
                long lstEnd = Utils.getMonthEndMillis(cal);
                
                double budget = 0, exp = 0, inc = 0, today = 0, lExp = 0, lInc = 0;
                List<TransactionWithUser> recents = new ArrayList<>();
                
                for (TransactionWithUser tu : allTransactionsList) {
                    Transaction t = tu.getTransaction();
                    String type = t.getType();
                    long ts = t.getTimestampMillis();
                    
                    boolean isCurMonth = ts >= curStart && ts <= curEnd;
                    boolean isLstMonth = ts >= lstStart && ts <= lstEnd;
                    
                    if ("ALLOCATION".equalsIgnoreCase(type)) {
                        if (t.getAllocationMonth() != null && t.getAllocationMonth().contains(curMonthQuery)) {
                            budget += t.getAmount();
                        }
                    } else if ("EXPENSE".equalsIgnoreCase(type)) {
                        if (isCurMonth) exp += t.getAmount();
                        if (isLstMonth) lExp += t.getAmount();
                        if (ts >= todayStart) today += t.getAmount();
                    } else if ("INCOME".equalsIgnoreCase(type)) {
                        if (isCurMonth) inc += t.getAmount();
                        if (isLstMonth) lInc += t.getAmount();
                    }
                }
                
                // Get 5 most recent
                recents = allTransactionsList.stream().limit(5).collect(java.util.stream.Collectors.toList());
                this.currentTransactions = recents;

                double bal = budget - exp;
                this.currentDash = new DashboardState(bal, inc, exp, bal, calculateTrend(inc, lInc), calculateTrend(exp, lExp), 0, today, budget);
                if (aiInsight == null) mainHandler.post(this::checkAiTrigger);
            }

            List<BaseMultiItem> items = new ArrayList<>();
            if (currentDash != null) items.add(new HomeListItem.DashboardItem(currentDash));
            
            if (isAiFetching) {
                items.add(new HomeListItem.AIInsightItem(
                        "Analyzing your Carti finances... 🤔",
                        "INFO"
                ));
            } else if (aiInsight != null && !aiInsight.isEmpty()) {
                try {
                    String cleanJson = aiInsight.replaceAll("```json", "").replaceAll("```", "").trim();
                    org.json.JSONArray arr = new org.json.JSONArray(cleanJson);
                    if (arr.length() > 0) {
                        for (int i = 0; i < arr.length(); i++) {
                            org.json.JSONObject obj = arr.getJSONObject(i);
                            String msg = obj.optString("message", obj.optString("text"));
                            items.add(new HomeListItem.AIInsightItem(msg, obj.optString("type")));
                        }
                    } else {
                        items.add(new HomeListItem.AIInsightItem("Laging tandaan: Ang pag-iipon ay para sa iyong future! 🙌", "INFO"));
                    }
                } catch (Exception e) {
                    String cleanMsg = aiInsight.replaceAll("[\\*\\[\\]]", "").trim();
                    if (!cleanMsg.isEmpty()) {
                        items.add(new HomeListItem.AIInsightItem(cleanMsg, "INFO"));
                    } else {
                        items.add(new HomeListItem.AIInsightItem("Kakatapos ko lang mag-analyze, check mo dashboard natin! 🙌", "INFO"));
                    }
                }
            }

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

                logs.sort((a, b) -> {
                    if (a.getTitle().equalsIgnoreCase("Others")) return 1;
                    if (b.getTitle().equalsIgnoreCase("Others")) return -1;
                    return a.getTitle().compareToIgnoreCase(b.getTitle());
                });

                items.add(new HomeListItem.QuickLogItemContainer(logs, null, null));
            }
            items.add(new HomeListItem.SectionHeaderItem(getApplication().getString(R.string.recent_activity), null, currentTransactions != null && !currentTransactions.isEmpty(), "View All", null));
            
            Boolean isSyncing = transRepo.getSyncingStatus().getValue();
            if (Boolean.TRUE.equals(isSyncing) && (currentTransactions == null || currentTransactions.isEmpty())) {
                items.add(new HomeListItem.ShimmerItem("1"));
                items.add(new HomeListItem.ShimmerItem("2"));
                items.add(new HomeListItem.ShimmerItem("3"));
            } else if (currentTransactions == null || currentTransactions.isEmpty()) {
                items.add(new HomeListItem.EmptyStateItem(getApplication().getString(R.string.no_transactions_yet)));
            } else {
                for (TransactionWithUser t : currentTransactions) items.add(new HomeListItem.TransactionItem(t, null));
            }

            if (instant) uiState.setValue(items);
            else mainHandler.post(() -> uiState.setValue(items));
        };

        if (instant) task.run();
        else executor.execute(task);
    }

    private double calculateTrend(double c, double p) { return p == 0 ? 0 : ((c - p) / p) * 100; }
    public record DashboardState(double balance, double monthlyIncome, double monthlyExpense, double monthlySavings, double incomeTrend, double expenseTrend, double savingsTrend, double todayExpense, double monthlyBudget) {}

    public void toggleLike(TransactionWithUser item) {
        transRepo.toggleLike(item, "👍");
    }

    public void toggleReaction(TransactionWithUser item, String emoji) {
        transRepo.toggleLike(item, emoji);
    }

    public void deleteTransaction(TransactionWithUser item) {
        Transaction transaction = item.getTransaction();
        transRepo.deleteItem(transaction.getType(), transaction.getId(), null);
    }
}
