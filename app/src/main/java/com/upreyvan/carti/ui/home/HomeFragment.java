package com.upreyvan.carti.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.databinding.ItemBillDueCardBinding;
import com.upreyvan.carti.databinding.ItemQuickActionBinding;
import com.upreyvan.carti.databinding.ItemQuickLogBinding;
import com.upreyvan.carti.databinding.ViewDueDateBillsBinding;
import com.upreyvan.carti.model.Bill;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.QuickLogItem;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.ui.bills.BillDetailsBottomSheet;
import com.upreyvan.carti.ui.budget.AddBudgetPlanActivity;
import com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;
import com.upreyvan.carti.ui.track.AllTransactionsFragment;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {
    private GenericAdapter<QuickLogItem, ItemQuickLogBinding> quickLogAdapter;
    private GenericAdapter<QuickLogItem, ItemQuickActionBinding> quickActionsAdapter;
    private GenericAdapter<Bill, ItemBillDueCardBinding> dueBillsAdapter;
    private TransactionAdapter transactionAdapter;
    private HomeViewModel viewModel;
    private ViewDueDateBillsBinding dueBillsBinding;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        if (getActivity() instanceof MainActivity main) {
            main.setBottomNavVisibility(false);
        }
        
        setupDynamicPadding(getBinding().layoutHeader, getBinding().home, 0.3f);
        
        // Critical Initialization (Rank 3: Priority)
        initAdapters();
        setupHeaders();
        setupRecentTransactions();
        
        transactionAdapter.setLoading(true);

        Utils.applySystemBarInsets(getBinding().layoutHeader, null, 0.85f, 0);
        
        observeViewModel();
        observeRealtime();
        setupBudgetPlanPrompt();

        // Non-Critical Initialization (Rank 3: Deferred for UI Thread breathing room)
        view.post(() -> {
            if (!isAdded()) return;
            setupQuickActions();
            setupQuickLog();
            setupNotifications();
        });

        BudgetManager.getInstance(requireContext()).addListener(items -> 
                requireActivity().runOnUiThread(() -> {
                    updateQuickLogData();
                    updateVisibilityBasedOnBudget();
                }));
    }

    private void ensureDueBillsInflated() {
        if (dueBillsBinding == null) {
            View inflated = getBinding().viewDueDateBills.inflate();
            dueBillsBinding = ViewDueDateBillsBinding.bind(inflated);
            dueBillsBinding.headerDueBills.tvSectionTitle.setText(R.string.due_bills_header);
            dueBillsBinding.rvDueBills.setAdapter(dueBillsAdapter);
        }
    }

    private void observeViewModel() {
        viewModel.getDashboardState().observe(getViewLifecycleOwner(), this::updateDashboardUI);
        
        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
            transactionAdapter.setLoading(false);
            transactionAdapter.submitList(transactions);
            boolean empty = transactions == null || transactions.isEmpty();
            getBinding().viewHeaderRecent.btnSectionAction.setVisibility(empty ? View.GONE : View.VISIBLE);
            getBinding().rvTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
            getBinding().tvNoTransactions.setVisibility(empty ? View.VISIBLE : View.GONE);
            
            if (getActivity() instanceof MainActivity main) {
                main.setBottomNavVisibility(true);
            }
        });

        viewModel.getDueBills().observe(getViewLifecycleOwner(), bills -> {
            boolean hasBills = bills != null && !bills.isEmpty();
            if (hasBills) {
                ensureDueBillsInflated();
                dueBillsBinding.getRoot().setVisibility(View.VISIBLE);
                dueBillsAdapter.submitList(bills);
            } else if (dueBillsBinding != null) {
                dueBillsBinding.getRoot().setVisibility(View.GONE);
            }
        });
    }

    private void updateDashboardUI(HomeViewModel.DashboardState state) {
        getBinding().viewHomeDashboard.tvBalanceAmount.setText(Utils.formatCurrency(state.balance()));
        getBinding().viewHomeDashboard.tvIncomeAmount.setText(getString(R.string.format_currency_no_decimal, state.monthlyIncome()));
        getBinding().viewHomeDashboard.tvExpensesAmount.setText(getString(R.string.format_currency_no_decimal, state.monthlyExpense()));
        getBinding().viewHomeDashboard.tvTotalSavings.setText(getString(R.string.format_currency_no_decimal, state.monthlySavings()));
        
        updateTrend(getBinding().viewHomeDashboard.tvIncomeTrend, state.incomeTrend(), false);
        updateTrend(getBinding().viewHomeDashboard.tvExpensesTrend, state.expenseTrend(), true);
        updateTrend(getBinding().viewHomeDashboard.tvSavingsTrend, state.savingsTrend(), false);
        
        getBinding().viewHomeDashboard.tvOverviewDate.setText(Utils.formatMonthYear(Calendar.getInstance()));
        setupHeaders(); 
    }

    private void observeRealtime() {
        RealtimeRepository realtime = RealtimeRepository.getInstance(requireContext());
        
        realtime.getFamilyStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                new PreferenceManager(requireContext()).saveFamilySummary(
                    Utils.getDouble(payload.get("balance")),
                    Utils.getDouble(payload.get("totalIncome")),
                    Utils.getDouble(payload.get("totalExpense"))
                );
            }
        });
    }

    private void initAdapters() {
        quickLogAdapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickLogBinding.inflate(i, p, false), (b, item) -> {
            b.tvLabel.setText(item.getTitle());
            b.ivIcon.setImageResource(item.getIconRes());
            int color = ContextCompat.getColor(requireContext(), item.getIconColor());
            b.cvIconBg.setCardBackgroundColor(ColorUtils.setAlphaComponent(color, 25));
            b.ivIcon.setColorFilter(color);
        });
        quickLogAdapter.setOnItemClickListener(item -> QuickLogsBottomSheetFragment.newInstance(item.getTitle()).show(getChildFragmentManager(), "QUICK_LOG"));
        quickLogAdapter.setOnItemLongClickListener(item -> { 
            startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)); 
            return true; 
        });

        quickActionsAdapter = new GenericAdapter<>(QuickLogItem.DIFF_CALLBACK, (i, p) -> ItemQuickActionBinding.inflate(i, p, false), (b, item) -> {
            b.tvLabel.setText(item.getTitle());
            b.ivIcon.setImageResource(item.getIconRes());
            b.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(requireContext(), item.getBgColor()));
            b.ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), item.getIconColor()));
        });
        
        quickActionsAdapter.setOnItemClickListener(item -> {
            MainActivity main = (MainActivity) getActivity();
            if (main == null) return;
            String title = item.getTitle();
            if (title.equals(getString(R.string.add_options_expense))) main.navigateTo(7);
            else if (title.equals(getString(R.string.action_add_income))) main.navigateTo(8);
            else if (title.equals(getString(R.string.action_family_chat))) main.navigateTo(5);
            else if (title.equals(getString(R.string.action_manage_goals))) main.navigateTo(6);
        });

        transactionAdapter = new TransactionAdapter();
        dueBillsAdapter = new GenericAdapter<>(Bill.DIFF_CALLBACK, (i, p) -> ItemBillDueCardBinding.inflate(i, p, false), (b, item) -> {
            b.tvBillName.setText(item.getName());
            b.tvDueDate.setText(item.getDate());
            b.ivIcon.setImageResource(item.getIconResId());
            b.tvStatus.setText(item.getStatus());
            b.getRoot().setOnClickListener(v -> BillDetailsBottomSheet.newInstance(item.getId(), item.getName()).show(getChildFragmentManager(), "BillDetails"));
        });
    }

    private void updateTrend(TextView textView, double percentage, boolean isExpense) {
        textView.setText(String.format(Locale.getDefault(), "%s%.1f%%", percentage >= 0 ? "+" : "", percentage));
        int colorRes = (percentage >= 0) ? (isExpense ? R.color.status_red : R.color.status_green) 
                                         : (isExpense ? R.color.status_green : R.color.status_red);
        if (percentage == 0) colorRes = R.color.text_secondary;
        textView.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void setupHeaders() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        getBinding().tvGreetingMain.setText(getString(R.string.format_greeting, Utils.getGreeting()));
        getBinding().tvUsernameMain.setText(getString(R.string.format_username, pref.getUsername()));
        
        SalaryManager sm = SalaryManager.getInstance(requireContext());
        String daysToGo = getString(R.string.days_to_go, sm.getDaysUntilNextPayday());
        String nextPayday = Utils.formatDateShort(sm.getNextPayday());
        getBinding().tvGreetingSub.setText(String.format("%s • %s", daysToGo, nextPayday));
        getBinding().tvGreetingSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
        
        getBinding().viewHeaderQuickActions.tvSectionTitle.setText(R.string.quick_actions_title);
        getBinding().viewHeaderQuickLog.tvSectionTitle.setText(R.string.quick_log_title);
        getBinding().viewHeaderQuickLog.tvSectionSubTitle.setText(R.string.quick_log_subtitle);
        getBinding().viewHeaderRecent.tvSectionTitle.setText(R.string.recent_activity);
        getBinding().viewHeaderRecent.btnSectionAction.setOnClickListener(v -> navigateTo(AllTransactionsFragment.newInstance(null)));
    }

    private void updateQuickLogData() {
        List<BudgetCategoryItem> plan = BudgetManager.getInstance(requireContext()).getBudgetPlan();
        List<QuickLogItem> items = new ArrayList<>();
        for (BudgetCategoryItem item : plan) {
            items.add(new QuickLogItem(item.getCategoryName(), item.getIconRes(), item.getBgColor(), item.getIconColor()));
        }
        quickLogAdapter.submitList(items);
        updateVisibilityBasedOnBudget();
    }

    private void updateVisibilityBasedOnBudget() {
        boolean hasPlan = !BudgetManager.getInstance(requireContext()).getBudgetPlan().isEmpty();
        int vis = hasPlan ? View.VISIBLE : View.GONE;
        getBinding().viewBudgetPlanPrompt.cardBudgetPlan.setVisibility(hasPlan ? View.GONE : View.VISIBLE);
        getBinding().viewHomeDashboard.getRoot().setVisibility(vis);
        
        if (dueBillsBinding != null) {
            dueBillsBinding.getRoot().setVisibility(vis);
        }
        
        getBinding().tvGreetingSub.setVisibility(vis);
        getBinding().viewHeaderQuickActions.getRoot().setVisibility(vis);
        getBinding().rvQuickActions.setVisibility(vis);
        getBinding().viewHeaderQuickLog.getRoot().setVisibility(vis);
        getBinding().rvQuickLog.setVisibility(vis);
    }

    private void setupQuickLog() { 
        // Rank 2: Performance tuning
        getBinding().rvQuickLog.setHasFixedSize(true);
        getBinding().rvQuickLog.setAdapter(quickLogAdapter); 
        updateQuickLogData(); 
    }

    private void setupQuickActions() {
        List<QuickLogItem> actions = new ArrayList<>();
        actions.add(new QuickLogItem(getString(R.string.add_options_expense), R.drawable.ic_add, R.color.status_red_tonal, R.color.status_red));
        actions.add(new QuickLogItem(getString(R.string.action_add_income), R.drawable.ic_arrow_up, R.color.dash_green_alpha, R.color.dash_green));
        actions.add(new QuickLogItem(getString(R.string.action_family_chat), R.drawable.ic_sync, R.color.log_fare, R.color.carti_primary_blue));
        actions.add(new QuickLogItem(getString(R.string.action_manage_goals), R.drawable.ic_trophy, R.color.mint_green_alpha, R.color.mint_green));
        getBinding().rvQuickActions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvQuickActions.setAdapter(quickActionsAdapter);
        quickActionsAdapter.submitList(actions);
    }

    private void setupRecentTransactions() {
        // Rank 2: Minimal performance tuning
        getBinding().rvTransactions.setHasFixedSize(true);
        getBinding().rvTransactions.setItemViewCacheSize(10);

        transactionAdapter.setOnTransactionInteractionListener(new TransactionAdapter.OnTransactionInteractionListener() {
            @Override 
            public void onLikeClick(TransactionWithUser item) { 
                viewModel.refreshData(); 
            }
            @Override 
            public void onReactionClick(TransactionWithUser item, String emoji) { }
            @Override 
            public void onCommentClick(Transaction t) { 
                CommentsBottomSheetFragment.newInstance(t.getId()).show(getChildFragmentManager(), "Comments"); 
            }
            @Override 
            public void onViewLikesClick(Transaction t, String names) { 
                ReactionsBottomSheetFragment.newInstance(t.getId()).show(getChildFragmentManager(), "Reactions"); 
            }
        });
        getBinding().rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTransactions.setAdapter(transactionAdapter);
    }

    private void setupNotifications() { 
        getBinding().btnNotif.setOnClickListener(v -> navigateTo(new NotificationsFragment())); 
    }

    private void setupBudgetPlanPrompt() { 
        getBinding().viewBudgetPlanPrompt.btnSetNow.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddBudgetPlanActivity.class))); 
    }

    @Override 
    public void onResume() { 
        super.onResume(); 
        viewModel.refreshData(); 
        updateQuickLogData(); 
    }
}
