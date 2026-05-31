package com.upreyvan.carti.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.ai.AiManager;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.AiRepository;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.databinding.ItemAiSuggestionCardBinding;
import com.upreyvan.carti.databinding.ItemQuickActionBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.databinding.ItemBillDueCardBinding;
import com.upreyvan.carti.model.AiSuggestion;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment;
import com.upreyvan.carti.ui.bills.BillDetailsBottomSheet;
import com.upreyvan.carti.ui.track.AllTransactionsFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.Utils;
import androidx.recyclerview.widget.DiffUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import com.upreyvan.carti.ui.budget.AddBudgetPlanActivity;
import androidx.core.graphics.ColorUtils;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {
    private GenericAdapter<QuickLogItem, ItemQuickLogBinding> quickLogAdapter;
    private GenericAdapter<QuickLogItem, ItemQuickActionBinding> quickActionsAdapter;
    private GenericAdapter<AiSuggestion, ItemAiSuggestionCardBinding> aiSuggestionsAdapter;
    private GenericAdapter<Bill, ItemBillDueCardBinding> dueBillsAdapter;
    private TransactionAdapter transactionAdapter;
    private RealtimeRepository realtimeRepo;
    private TransactionRepository transactionRepository;
    private MemberRepository memberRepository;
    private AiRepository aiRepository;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
        memberRepository = new MemberRepository(requireContext());
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        aiRepository = AiRepository.getInstance(requireContext());
        
        setupDynamicPadding(getBinding().layoutHeader, getBinding().home, 0.3f);
        initAdapters();
        setupHeaders();
        setupQuickActions();
        setupQuickLog();
        setupRecentTransactions();
        setupNotifications();
        
        observeTransactions();
        observeRealtimeData();
        setupDashboard();
        setupBudgetPlanPrompt();
        setupAiInsightCard();
        setupAiSuggestions();
        fetchFamilyData();
        loadHomeAiInsights();

        BudgetManager.getInstance(requireContext()).addListener(items -> {
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                updateQuickLogData();
                updateVisibilityBasedOnBudget();
            });
        });
    }

    private void observeRealtimeData() {
        realtimeRepo.getTransactionStream().observe(getViewLifecycleOwner(), payload -> transactionRepository.refreshTransactions());
        realtimeRepo.getUserUpdateStream().observe(getViewLifecycleOwner(), payload -> {
            checkNotifications(new ApiHelper(requireContext()), new PreferenceManager(requireContext()), null);
            memberRepository.refreshMembers();
        });
        realtimeRepo.getFamilyStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                PreferenceManager pref = new PreferenceManager(requireContext());
                pref.saveFamilySummary(Utils.getDouble(payload.get("balance")), Utils.getDouble(payload.get("totalIncome")), Utils.getDouble(payload.get("totalExpense")));
                requireActivity().runOnUiThread(() -> { setupHeaders(); setupDashboard(); });
            }
        });
        realtimeRepo.getNotificationStream().observe(getViewLifecycleOwner(), payload -> {
            checkNotifications(new ApiHelper(requireContext()), new PreferenceManager(requireContext()), null);
            setupDueBills();
        });
        realtimeRepo.getLikeStream().observe(getViewLifecycleOwner(), payload -> transactionRepository.refreshTransactions());
        setupDueBills();
    }

    private void setupDueBills() {
        new ApiHelper(requireContext()).getNotifications(new PreferenceManager(requireContext()).getFamilyId(), new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (!isAdded()) return;
                List<Bill> dueBills = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    String title = (String) doc.getData().get("title");
                    if (title != null && title.startsWith("BILL: ")) {
                        dueBills.add(new Bill(doc.getId(), "", title.substring(6), (String) doc.getData().get("content"), "Unpaid", R.drawable.ic_calendar));
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    getBinding().viewDueDateBills.getRoot().setVisibility(dueBills.isEmpty() ? View.GONE : View.VISIBLE);
                    dueBillsAdapter.submitList(dueBills);
                });
            }
            @Override public void onError(Throwable error) { requireActivity().runOnUiThread(() -> getBinding().viewDueDateBills.getRoot().setVisibility(View.GONE)); }
        });
    }

    private void updateQuickLogData() {
        List<com.upreyvan.carti.model.BudgetCategoryItem> budgetPlan = BudgetManager.getInstance(requireContext()).getBudgetPlan();
        List<QuickLogItem> items = new ArrayList<>();
        for (com.upreyvan.carti.model.BudgetCategoryItem item : budgetPlan) items.add(new QuickLogItem(item.getCategoryName(), item.getIconRes(), item.getBgColor(), item.getIconColor()));
        getBinding().viewHeaderQuickLog.getRoot().setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        getBinding().rvQuickLog.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        quickLogAdapter.submitList(items);
    }

    private void initAdapters() {
        quickLogAdapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickLogBinding.inflate(i, p, false), (b, item) -> {
            b.tvLabel.setText(item.getTitle()); b.ivIcon.setImageResource(item.getIconRes());
            int color = ContextCompat.getColor(requireContext(), item.getIconColor());
            b.cvIconBg.setCardBackgroundColor(ColorUtils.setAlphaComponent(color, 25)); b.ivIcon.setColorFilter(color);
        });
        quickLogAdapter.setOnItemClickListener(this::showQuickLogDialog);
        quickLogAdapter.setOnItemLongClickListener(item -> { startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)); return true; });

        quickActionsAdapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickActionBinding.inflate(i, p, false), (b, item) -> {
            b.tvLabel.setText(item.getTitle()); b.ivIcon.setImageResource(item.getIconRes());
            b.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(requireContext(), item.getBgColor())); b.ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), item.getIconColor()));
        });
        quickActionsAdapter.setOnItemClickListener(item -> {
            MainActivity main = (MainActivity) getActivity();
            if (item.getTitle().equals(getString(R.string.add_options_expense))) main.navigateTo(7);
            else if (item.getTitle().equals(getString(R.string.action_add_income))) main.navigateTo(8);
            else if (item.getTitle().equals(getString(R.string.action_family_chat))) main.navigateTo(5);
            else if (item.getTitle().equals(getString(R.string.action_manage_goals))) main.navigateTo(6);
        });

        transactionAdapter = new TransactionAdapter();
        dueBillsAdapter = new GenericAdapter<>(Bill.DIFF_CALLBACK, (i, p) -> ItemBillDueCardBinding.inflate(i, p, false), (b, item) -> {
            b.tvBillName.setText(item.getName()); b.tvDueDate.setText(item.getDate()); b.ivIcon.setImageResource(item.getIconResId()); b.tvStatus.setText(item.getStatus());
            b.getRoot().setOnClickListener(v -> BillDetailsBottomSheet.newInstance(item.getId(), item.getName()).show(getChildFragmentManager(), "BillDetailsBottomSheet"));
        });
    }

    private void setupAiSuggestions() {
        aiSuggestionsAdapter = new GenericAdapter<>(new DiffUtil.ItemCallback<AiSuggestion>() {
            @Override public boolean areItemsTheSame(@NonNull AiSuggestion o, @NonNull AiSuggestion n) { return o.getTitle().equals(n.getTitle()); }
            @Override public boolean areContentsTheSame(@NonNull AiSuggestion o, @NonNull AiSuggestion n) { return o.getDescription().equals(n.getDescription()); }
        }, (i, p) -> ItemAiSuggestionCardBinding.inflate(i, p, false), (b, item) -> {
            b.tvTitle.setText(item.getTitle()); b.tvDescription.setText(item.getDescription()); b.ivIcon.setImageResource(item.getIconResId());
            b.cvIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), item.getThemeColor())); b.btnAction.setText(item.getActionText());
            b.btnAction.setTextColor(ContextCompat.getColor(requireContext(), item.getThemeColor()));
            b.btnClose.setOnClickListener(v -> { List<AiSuggestion> list = new ArrayList<>(aiSuggestionsAdapter.getCurrentList()); list.remove(item); aiSuggestionsAdapter.submitList(list); });
            b.btnAction.setOnClickListener(v -> handleAiSuggestionAction(item));
            b.getRoot().setOnClickListener(v -> handleAiSuggestionAction(item));
        });
        getBinding().viewAiSuggestions.rvAiSuggestions.setAdapter(aiSuggestionsAdapter);
        getBinding().viewAiSuggestions.btnCloseContainer.setOnClickListener(v -> getBinding().viewAiSuggestions.getRoot().setVisibility(View.GONE));
        loadAiSuggestions();
    }

    private void handleAiSuggestionAction(AiSuggestion item) {
        MainActivity main = (MainActivity) getActivity();
        String type = item.getType();
        if ("SAVINGS".equalsIgnoreCase(type) || "EXPENSE".equalsIgnoreCase(type) || "BILL".equalsIgnoreCase(type)) main.navigateTo(2);
        else if ("GOAL".equalsIgnoreCase(type)) main.navigateTo(6);
        else main.navigateTo(5);
    }

    private void loadAiSuggestions() {
        aiRepository.getSmartSuggestions(new AiManager.AiCallback() {
            @Override public void onSuccess(String response) {
                if (!isAdded()) return;
                try {
                    List<Map<String, String>> rawList = new com.google.gson.Gson().fromJson(response, new com.google.gson.reflect.TypeToken<ArrayList<Map<String, String>>>() {}.getType());
                    List<AiSuggestion> suggestions = new ArrayList<>();
                    for (Map<String, String> raw : rawList) {
                        String type = raw.get("type"); int icon = R.drawable.ic_chart, color = R.color.carti_primary_blue;
                        if ("SAVINGS".equalsIgnoreCase(type)) { icon = R.drawable.ic_trophy; color = R.color.status_green; }
                        else if ("EXPENSE".equalsIgnoreCase(type)) { icon = R.drawable.ic_chart; color = R.color.status_red; }
                        else if ("BILL".equalsIgnoreCase(type)) { icon = R.drawable.ic_calendar; color = R.color.carti_primary_blue; }
                        else if ("GOAL".equalsIgnoreCase(type)) { icon = R.drawable.ic_sync; color = R.color.mint_green; }
                        suggestions.add(new AiSuggestion(raw.get("title"), raw.get("description"), icon, color, raw.get("actionText"), type));
                    }
                    requireActivity().runOnUiThread(() -> { getBinding().viewAiSuggestions.getRoot().setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE); aiSuggestionsAdapter.submitList(suggestions); });
                } catch (Exception ignored) {}
            }
            @Override public void onError(Throwable t) { if (isAdded()) requireActivity().runOnUiThread(() -> getBinding().viewAiSuggestions.getRoot().setVisibility(View.GONE)); }
        });
    }

    private void setupAiInsightCard() {
        getBinding().viewAiInsight.btnAskAi.setOnClickListener(v -> { if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateTo(5); });
    }

    private void loadHomeAiInsights() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        String cached = pref.getDailyAiInsightText();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());

        if (today.equals(pref.getDailyAiInsightDate()) && !cached.isEmpty()) {
            getBinding().viewAiInsight.tvAiMessage.setText(cached);
            getBinding().viewAiInsight.tvAiMessage.setAlpha(1.0f);
            return;
        }

        getBinding().viewAiInsight.tvAiMessage.setText(R.string.ai_insight_loading);
        getBinding().viewAiInsight.tvAiMessage.setAlpha(0.6f);
        aiRepository.getDailyInsights(new AiManager.AiCallback() {
            @Override public void onSuccess(String response) {
                if (isAdded()) requireActivity().runOnUiThread(() -> { pref.saveDailyAiInsight(today, response); getBinding().viewAiInsight.tvAiMessage.setText(response); getBinding().viewAiInsight.tvAiMessage.setAlpha(1.0f); });
            }
            @Override public void onError(Throwable t) { if (isAdded()) requireActivity().runOnUiThread(() -> getBinding().viewAiInsight.tvAiMessage.setText(R.string.ai_insight_error)); }
        });
    }

    private void observeTransactions() {
        transactionRepository.getRecentTransactions(5).observe(getViewLifecycleOwner(), transactions -> {
            if (transactionAdapter != null) {
                transactionAdapter.setLoading(false); transactionAdapter.submitList(transactions);
                boolean empty = transactions == null || transactions.isEmpty();
                getBinding().viewHeaderRecent.getRoot().setVisibility(empty ? View.GONE : View.VISIBLE);
                getBinding().cardRecentTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
                getBinding().rvTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
                getBinding().tvNoTransactions.setVisibility(empty ? View.VISIBLE : View.GONE);
                getBinding().viewHeaderRecent.btnSectionAction.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });
        transactionRepository.syncTransactionsIfNeeded();
    }

    private void fetchFamilyData() {
        new ApiHelper(requireContext()).getFamilySummary(new AppwriteManager.AppwriteCallback<Document<Map<String, Object>>>() {
            @Override public void onSuccess(Document<Map<String, Object>> result) {
                if (!isAdded()) return; Map<String, Object> data = result.getData();
                PreferenceManager pref = new PreferenceManager(requireContext());
                pref.saveFamilySummary(Utils.getDouble(data.get("balance")), Utils.getDouble(data.get("totalIncome")), Utils.getDouble(data.get("totalExpense")));
                pref.setAdminId(String.valueOf(data.get("adminId"))); checkNotifications(new ApiHelper(requireContext()), pref, pref.getAdminId());
                requireActivity().runOnUiThread(() -> { setupHeaders(); setupDashboard(); });
            }
            @Override public void onError(Throwable error) { checkNotifications(new ApiHelper(requireContext()), new PreferenceManager(requireContext()), null); }
        });
    }

    private void checkNotifications(ApiHelper api, PreferenceManager pref, String adminId) {
        boolean isAdmin = (adminId != null && !adminId.isEmpty()) ? pref.getUserId().equals(adminId) : false;
        if (!isAdmin) { for (String r : Constants.Roles.PARENTS) if (r.equalsIgnoreCase(pref.getUserRole())) { isAdmin = true; break; } }
        if (isAdmin) {
            api.getPendingMembers(new AppwriteManager.AppwriteCallback<io.appwrite.models.DocumentList<Map<String, Object>>>() {
                @Override public void onSuccess(io.appwrite.models.DocumentList<Map<String, Object>> result) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> { int count = result.getDocuments().size(); updateNotificationBadge(count > 0);
                        if (count > 0) showFamilyNotificationCard(count); else getBinding().viewFamilyNotification.cardNotification.setVisibility(View.GONE); });
                }
                @Override public void onError(Throwable e) { requireActivity().runOnUiThread(() -> updateNotificationBadge(pref.hasNotifications())); }
            });
        } else { updateNotificationBadge(false); requireActivity().runOnUiThread(() -> getBinding().viewFamilyNotification.cardNotification.setVisibility(View.GONE)); }
    }

    private void showFamilyNotificationCard(int count) {
        getBinding().viewFamilyNotification.cardNotification.setVisibility(View.VISIBLE);
        getBinding().viewFamilyNotification.tvTitle.setText(getString(R.string.pending_requests_title, count));
        getBinding().viewFamilyNotification.tvContent.setText(R.string.pending_requests_desc);
        getBinding().viewFamilyNotification.ivIcon.setImageResource(R.drawable.ic_person);
        getBinding().viewFamilyNotification.getRoot().setOnClickListener(v -> { if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateTo(4); });
        getBinding().viewFamilyNotification.btnClose.setOnClickListener(v -> getBinding().viewFamilyNotification.cardNotification.setVisibility(View.GONE));
    }

    private void setupDashboard() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        getBinding().viewHomeDashboard.tvBalanceAmount.setText(Utils.formatCurrency(pref.getBalance()));
        Calendar current = Calendar.getInstance(); Calendar lastMonth = Calendar.getInstance(); lastMonth.add(Calendar.MONTH, -1);
        long curStart = Utils.getMonthStartMillis(current), curEnd = Utils.getMonthEndMillis(current);
        long lstStart = Utils.getMonthStartMillis(lastMonth), lstEnd = Utils.getMonthEndMillis(lastMonth);
        transactionRepository.getTotalIncomeInRange(curStart, curEnd).observe(getViewLifecycleOwner(), income -> {
            double curInc = income != null ? income : 0.0; getBinding().viewHomeDashboard.tvIncomeAmount.setText(getString(R.string.format_currency_no_decimal, curInc));
            transactionRepository.getTotalIncomeInRange(lstStart, lstEnd).observe(getViewLifecycleOwner(), lastIncome -> updateTrend(getBinding().viewHomeDashboard.tvIncomeTrend, curInc, lastIncome != null ? lastIncome : 0.0));
        });
        transactionRepository.getTotalExpenseInRange(curStart, curEnd).observe(getViewLifecycleOwner(), expense -> {
            double curExp = expense != null ? expense : 0.0; getBinding().viewHomeDashboard.tvExpensesAmount.setText(getString(R.string.format_currency_no_decimal, curExp));
            transactionRepository.getTotalExpenseInRange(lstStart, lstEnd).observe(getViewLifecycleOwner(), lastExpense -> updateTrend(getBinding().viewHomeDashboard.tvExpensesTrend, curExp, lastExpense != null ? lastExpense : 0.0));
        });
        transactionRepository.getTotalIncomeInRange(curStart, curEnd).observe(getViewLifecycleOwner(), income -> transactionRepository.getTotalExpenseInRange(curStart, curEnd).observe(getViewLifecycleOwner(), expense -> {
            double curInc = income != null ? income : 0.0, curExp = expense != null ? expense : 0.0, savings = curInc - curExp;
            getBinding().viewHomeDashboard.tvTotalSavings.setText(getString(R.string.format_currency_no_decimal, savings));
            transactionRepository.getTotalIncomeInRange(lstStart, lstEnd).observe(getViewLifecycleOwner(), lastIncome -> transactionRepository.getTotalExpenseInRange(lstStart, lstEnd).observe(getViewLifecycleOwner(), lastExpense -> updateTrend(getBinding().viewHomeDashboard.tvSavingsTrend, savings, (lastIncome != null ? lastIncome : 0.0) - (lastExpense != null ? lastExpense : 0.0))));
        }));
        getBinding().viewHomeDashboard.tvOverviewDate.setText(Utils.formatMonthYear(current));
    }

    private void updateTrend(android.widget.TextView textView, double current, double previous) {
        if (previous == 0) { textView.setText("0%"); textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary)); return; }
        double percentage = ((current - previous) / previous) * 100;
        textView.setText(String.format(Locale.getDefault(), "%s%.1f%%", percentage >= 0 ? "+" : "", percentage));
        int color = percentage >= 0 ? R.color.status_green : R.color.status_red;
        if (textView.getId() == getBinding().viewHomeDashboard.tvExpensesTrend.getId()) color = percentage <= 0 ? R.color.status_green : R.color.status_red;
        textView.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private void setupBudgetPlanPrompt() {
        updateVisibilityBasedOnBudget();
        getBinding().viewBudgetPlanPrompt.btnSetNow.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddBudgetPlanActivity.class)));
    }

    private void updateVisibilityBasedOnBudget() {
        boolean hasPlan = !BudgetManager.getInstance(requireContext()).getBudgetPlan().isEmpty();
        int vis = hasPlan ? View.VISIBLE : View.GONE;
        getBinding().viewBudgetPlanPrompt.cardBudgetPlan.setVisibility(hasPlan ? View.GONE : View.VISIBLE);
        getBinding().viewHomeDashboard.getRoot().setVisibility(vis); getBinding().viewAiInsight.getRoot().setVisibility(vis);
        getBinding().viewDueDateBills.getRoot().setVisibility(vis); getBinding().viewAiSuggestions.getRoot().setVisibility(vis);
        getBinding().tvGreetingSub.setVisibility(vis); getBinding().viewHeaderQuickActions.getRoot().setVisibility(vis);
        getBinding().rvQuickActions.setVisibility(vis); getBinding().viewHeaderQuickLog.getRoot().setVisibility(vis); getBinding().rvQuickLog.setVisibility(vis);
    }

    @Override public void onResume() { super.onResume(); setupQuickLog(); setupDashboard(); setupBudgetPlanPrompt(); updateNotificationBadge(new PreferenceManager(requireContext()).hasNotifications()); }

    private void updateNotificationBadge(boolean has) { getBinding().notifBadge.setVisibility(has ? View.VISIBLE : View.GONE); if (has) getBinding().notifBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.mint_green)); }

    private void setupNotifications() { getBinding().btnNotif.setOnClickListener(v -> navigateTo(new NotificationsFragment())); }

    private void setupHeaders() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        getBinding().tvGreetingMain.setText(getString(R.string.format_greeting, Utils.getGreeting()));
        getBinding().tvUsernameMain.setText(getString(R.string.format_username, pref.getUsername()));
        com.upreyvan.carti.data.local.SalaryManager sm = com.upreyvan.carti.data.local.SalaryManager.getInstance(requireContext());
        getBinding().tvGreetingSub.setText(getString(R.string.days_to_go, sm.getDaysUntilNextPayday()) + " • " + Utils.formatDateShort(sm.getNextPayday()));
        getBinding().tvGreetingSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
        getBinding().viewHeaderQuickActions.tvSectionTitle.setText(R.string.quick_actions_title); getBinding().viewHeaderQuickActions.btnSectionAction.setVisibility(View.GONE);
        getBinding().viewHeaderQuickLog.tvSectionTitle.setText(R.string.quick_log_title); getBinding().viewHeaderQuickLog.tvSectionSubTitle.setVisibility(View.VISIBLE);
        getBinding().viewHeaderQuickLog.tvSectionSubTitle.setText(R.string.quick_log_subtitle); getBinding().viewHeaderQuickLog.getRoot().setOnClickListener(v -> QuickLogsBottomSheetFragment.newInstance(null).show(getChildFragmentManager(), "QUICK_LOG_BOTTOM_SHEET"));
        getBinding().viewHeaderQuickLog.btnSectionAction.setText(R.string.customize); getBinding().viewHeaderQuickLog.btnSectionAction.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)));
        getBinding().viewHeaderRecent.tvSectionTitle.setText(R.string.recent_activity); getBinding().viewHeaderRecent.btnSectionAction.setText(R.string.see_all); getBinding().viewHeaderRecent.btnSectionAction.setOnClickListener(v -> navigateTo(AllTransactionsFragment.newInstance(null)));
    }

    private void setupQuickLog() { getBinding().rvQuickLog.setAdapter(quickLogAdapter); updateQuickLogData(); }

    private void setupQuickActions() {
        List<QuickLogItem> actions = new ArrayList<>();
        actions.add(new QuickLogItem(getString(R.string.add_options_expense), R.drawable.ic_add, R.color.status_red_tonal, R.color.status_red));
        actions.add(new QuickLogItem(getString(R.string.action_add_income), R.drawable.ic_arrow_up, R.color.dash_green_alpha, R.color.dash_green));
        actions.add(new QuickLogItem(getString(R.string.action_family_chat), R.drawable.ic_sync, R.color.log_fare, R.color.carti_primary_blue));
        actions.add(new QuickLogItem(getString(R.string.action_manage_goals), R.drawable.ic_trophy, R.color.mint_green_alpha, R.color.mint_green));
        getBinding().rvQuickActions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)); getBinding().rvQuickActions.setAdapter(quickActionsAdapter); quickActionsAdapter.submitList(actions);
    }

    private void showQuickLogDialog(QuickLogItem item) { com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment.newInstance(item.getTitle()).show(getChildFragmentManager(), "QUICK_LOG_BOTTOM_SHEET"); }

    private void setupRecentTransactions() {
        if (transactionAdapter == null) transactionAdapter = new TransactionAdapter();
        transactionAdapter.setOnTransactionInteractionListener(new TransactionAdapter.OnTransactionInteractionListener() {
            @Override public void onLikeClick(com.upreyvan.carti.model.Transaction t) { transactionRepository.likeTransaction(t.getId(), "👍", null); }
            @Override public void onReactionClick(com.upreyvan.carti.model.Transaction t, String e) { transactionRepository.likeTransaction(t.getId(), e, null); }
            @Override public void onCommentClick(com.upreyvan.carti.model.Transaction t) { CommentsBottomSheetFragment.newInstance(t.getId()).show(getChildFragmentManager(), "CommentsBottomSheet"); }
            @Override public void onViewLikesClick(com.upreyvan.carti.model.Transaction t, String names) { ReactionsBottomSheetFragment.newInstance(t.getId()).show(getChildFragmentManager(), "ReactionsBottomSheet"); }
        });
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext())); getBinding().rvTransactions.setAdapter(transactionAdapter);
        getBinding().viewDueDateBills.rvDueBills.setAdapter(dueBillsAdapter); getBinding().viewDueDateBills.headerDueBills.tvSectionTitle.setText(R.string.due_bills_header);
        getBinding().viewDueDateBills.headerDueBills.tvSectionSubTitle.setVisibility(View.GONE); getBinding().viewDueDateBills.headerDueBills.btnSectionAction.setVisibility(View.GONE);
    }
}
