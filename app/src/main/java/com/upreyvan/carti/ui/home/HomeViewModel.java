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
    private final com.upreyvan.carti.managers.ai.AiInsightEngine aiEngine;
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
        aiEngine = new com.upreyvan.carti.managers.ai.AiInsightEngine(application);
        pref = PreferenceManager.getInstance(application);
        setupDataStream();
        uiState.addSource(dataTrigger, v -> {});
    }

    public LiveData<List<BaseMultiItem>> getUiState() { return uiState; }

    public void refreshData() {
        // Only clear cache and show loading for manual refresh
        setLoading(true);
        pref.setAiInsightsCache(""); 
        aiInsight = null;
        
        transRepo.refreshTransactions();
        memberRepo.refreshMembers();
        fetchAiInsight();
    }

    private void setupDataStream() {
        dataTrigger.addSource(transRepo.getFinancialSummary(), v -> rebuild(false));
        dataTrigger.addSource(notifRepo.getBills(pref.getFamilyId()), v -> { this.currentBills = v; rebuild(false); });
        dataTrigger.addSource(transRepo.getBudgetPlanLiveData(), v -> { this.currentPlan = v; rebuild(false); });
        dataTrigger.addSource(transRepo.getSyncingStatus(), v -> rebuild(false));
    }

    private void checkAiTrigger() {
        if (aiInsight == null && !isAiFetching) {
            String cached = pref.getAiInsightsCache();
            long timestamp = pref.getAiInsightsTimestamp();
            
            // Production: 12 hours (12 * 60 * 60 * 1000)
            // Testing: 0 (refresh every app open)
            long refreshInterval = 0; 
            
            boolean isExpired = (System.currentTimeMillis() - timestamp) > refreshInterval;
            boolean isBoring = cached.toLowerCase().contains("no urgent") || 
                              cached.toLowerCase().contains("keep tracking"); // Mark static fallback as boring to force retry
            
            if (!cached.isEmpty() && !isExpired && !isBoring && refreshInterval > 0) {
                aiInsight = cached;
                rebuild(false);
            } else {
                fetchAiInsight();
            }
        }
    }

    private void fetchAiInsight() {
        if (isAiFetching) return;
        
        com.upreyvan.carti.models.FinancialSummary fs = transRepo.getFinancialSummary().getValue();
        if (fs == null) return;
        
        isAiFetching = true;
        rebuild(false); 

        aiEngine.generateInsights(fs, currentBills, new AiRepository.AiCallback() {
            @Override public void onSuccess(String response) {
                isAiFetching = false;
                setLoading(false);
                
                // Force parse to ensure it's not just a generic message
                try {
                    String cleanJson = response.replaceAll("```json", "").replaceAll("```", "").trim();
                    if (cleanJson.startsWith("[")) {
                        aiInsight = response;
                        pref.setAiInsightsCache(response);
                    } else {
                        // Transform plain text response into JSON format
                        aiInsight = "[{\"type\": \"INFO\", \"message\": \"" + response.replace("\"", "\\\"") + "\"}]";
                        pref.setAiInsightsCache(aiInsight);
                    }
                } catch (Exception e) {
                    aiInsight = response;
                    pref.setAiInsightsCache(response);
                }
                
                // Crucial: Update timestamp after a successful fetch to handle interval logic
                // But since we are in testing (interval 0), this ensures it knows when it last fetched.
                rebuild(false);
            }
            @Override public void onError(Throwable t) {
                isAiFetching = false;
                setLoading(false);
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
            long lastAiCheck = pref.getAiInsightsTimestamp();
            if (lastAiCheck > 0 && lastAiCheck < Utils.getStartOfDayMillis()) {
                aiInsight = null;
            }

            // Phase 4: Reuse Centralized Financial Summary
            com.upreyvan.carti.models.FinancialSummary fs = transRepo.getFinancialSummary().getValue();
            if (fs != null) {
                // Stabilize state by copying from centralized engine once
                this.currentDash = new DashboardState(
                    fs.monthlyBalance(), 
                    fs.monthlyIncome(), 
                    fs.monthlyExpense(), 
                    fs.totalAccumulatedSavings(),
                    fs.incomeTrend(), 
                    fs.expenseTrend(), 
                    fs.savingsTrend(), 
                    fs.todayExpense(), 
                    fs.monthlyBudget()
                );
                
                List<TransactionWithUser> all = transRepo.getAllTransactions().getValue();
                if (all != null) {
                    this.currentTransactions = all.stream().limit(5).collect(java.util.stream.Collectors.toList());
                }

                if (aiInsight == null) mainHandler.post(this::checkAiTrigger);
            }

            // Only rebuild UI if we have data to avoid flickering/excessive loading
            if (currentDash == null && !transRepo.getSyncingStatus().getValue()) {
                return;
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
                
                // Dashboard awareness: Show all UNPAID bills regardless of how far the due date is
                List<Bill> unpaidBills = currentBills.stream()
                        .filter(b -> !"Paid".equalsIgnoreCase(b.getStatus()))
                        .collect(java.util.stream.Collectors.toList());
                
                if (!unpaidBills.isEmpty()) {
                    items.add(new HomeListItem.BillContainerItem(unpaidBills, null, null));
                }
            }
            if (currentPlan != null && !currentPlan.isEmpty()) {
                items.add(new HomeListItem.SectionHeaderItem(getApplication().getString(R.string.quick_log_title), getApplication().getString(R.string.quick_log_subtitle), false, null, null));
                List<QuickLogItem> logs = new ArrayList<>();
                for (BudgetCategoryItem p : currentPlan) logs.add(new QuickLogItem(p.getCategoryName(), p.getIconRes(), p.getBgColor(), p.getIconColor()));

                // Centralized Sorting for Quick Logs on Home
                Utils.sortAlphabetically(logs, QuickLogItem::getTitle);

                items.add(new HomeListItem.QuickLogItemContainer(logs, null, null));
            }
            items.add(new HomeListItem.SectionHeaderItem(getApplication().getString(R.string.recent_activity), null, currentTransactions != null && !currentTransactions.isEmpty(), getApplication().getString(R.string.see_all), null));
            
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
