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
import com.upreyvan.carti.ui.track.AddBudgetPlanActivity;
import com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;
import com.upreyvan.carti.ui.track.AllTransactionsFragment;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> implements HomeAdapter.OnHomeInteractionListener {
    private HomeAdapter homeAdapter;
    private HomeViewModel viewModel;

    private HomeViewModel.DashboardState currentDashboardState;
    private List<Bill> currentBills;
    private List<TransactionWithUser> currentTransactions;

    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        if (getActivity() instanceof MainActivity main) {
            main.setBottomNavVisibility(true);
        }
        
        setupHeaders();
        initAdapter();
        
        Utils.applySystemBarInsets(getBinding().layoutHeader, null, 0.85f, 0);
        
        observeViewModel();
        observeRealtime();

        BudgetManager.getInstance(requireContext()).addListener(items -> 
                requireActivity().runOnUiThread(this::updateItems));
    }

    private void initAdapter() {
        homeAdapter = new HomeAdapter(this);
        getBinding().rvMainHome.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMainHome.setAdapter(homeAdapter);
    }

    private void observeViewModel() {
        viewModel.getDashboardState().observe(getViewLifecycleOwner(), state -> {
            this.currentDashboardState = state;
            updateItems();
        });
        
        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
            this.currentTransactions = transactions;
            updateItems();
        });

        viewModel.getDueBills().observe(getViewLifecycleOwner(), bills -> {
            this.currentBills = bills;
            updateItems();
        });
    }

    private void updateItems() {
        List<HomeListItem> items = new ArrayList<>();
        
        if (currentDashboardState != null) {
            items.add(new HomeListItem.DashboardItem(currentDashboardState));
        }

        items.add(new HomeListItem.AIInsightItem("You've spent ₱6,670 this month. Keep it up! 🙌"));

        boolean hasPlan = !BudgetManager.getInstance(requireContext()).getBudgetPlan().isEmpty();
        if (!hasPlan) {
            items.add(new HomeListItem.BudgetPromptItem());
        }

        if (currentBills != null && !currentBills.isEmpty()) {
            items.add(new HomeListItem.SectionHeaderItem(getString(R.string.due_bills_header), null, false, null));
            items.add(new HomeListItem.BillContainerItem(currentBills));
        }

        items.add(new HomeListItem.SectionHeaderItem(getString(R.string.quick_actions_title), null, false, null));
        List<QuickLogItem> actions = new ArrayList<>();
        actions.add(new QuickLogItem(getString(R.string.add_options_expense), R.drawable.ic_add, R.color.status_red_tonal, R.color.status_red));
        actions.add(new QuickLogItem(getString(R.string.action_add_income), R.drawable.ic_arrow_up, R.color.dash_green_alpha, R.color.dash_green));
        actions.add(new QuickLogItem(getString(R.string.action_family_chat), R.drawable.ic_sync, R.color.log_fare, R.color.carti_primary_blue));
        actions.add(new QuickLogItem(getString(R.string.action_manage_goals), R.drawable.ic_trophy, R.color.mint_green_alpha, R.color.mint_green));
        items.add(new HomeListItem.QuickActionsItem(actions));

        if (hasPlan) {
            items.add(new HomeListItem.SectionHeaderItem(getString(R.string.quick_log_title), getString(R.string.quick_log_subtitle), false, null));
            List<com.upreyvan.carti.model.BudgetCategoryItem> plan = BudgetManager.getInstance(requireContext()).getBudgetPlan();
            List<QuickLogItem> logs = new ArrayList<>();
            for (com.upreyvan.carti.model.BudgetCategoryItem planItem : plan) {
                logs.add(new QuickLogItem(planItem.getCategoryName(), planItem.getIconRes(), planItem.getBgColor(), planItem.getIconColor()));
            }
            items.add(new HomeListItem.QuickLogItemContainer(logs));
        }

        items.add(new HomeListItem.SectionHeaderItem(getString(R.string.recent_activity), null, currentTransactions != null && !currentTransactions.isEmpty(), "View All"));
        
        if (currentTransactions == null || currentTransactions.isEmpty()) {
            items.add(new HomeListItem.EmptyStateItem(getString(R.string.no_transactions_yet)));
        } else {
            for (TransactionWithUser t : currentTransactions) {
                items.add(new HomeListItem.TransactionItem(t));
            }
        }

        homeAdapter.submitList(items);
    }

    private void observeRealtime() {
        RealtimeRepository realtime = RealtimeRepository.getInstance(requireContext());
        realtime.getFamilyStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                PreferenceManager.getInstance(requireContext()).saveFamilySummary(
                    Utils.getDouble(payload.get("balance")),
                    Utils.getDouble(payload.get("totalIncome")),
                    Utils.getDouble(payload.get("totalExpense"))
                );
            }
        });
    }

    private void setupHeaders() {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        getBinding().tvGreetingMain.setText(getString(R.string.format_greeting, Utils.getGreeting()));
        getBinding().tvUsernameMain.setText(getString(R.string.format_username, pref.getUsername()));
        
        SalaryManager sm = SalaryManager.getInstance(requireContext());
        String daysToGo = getString(R.string.days_to_go, sm.getDaysUntilNextPayday());
        String nextPayday = Utils.formatDateShort(sm.getNextPayday());
        getBinding().tvGreetingSub.setText(String.format("%s • %s", daysToGo, nextPayday));
        getBinding().tvGreetingSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
    }

    @Override public void onBudgetPromptClick() { startActivity(new Intent(requireContext(), AddBudgetPlanActivity.class)); }
    @Override public void onBillClick(Bill bill) { BillDetailsBottomSheet.newInstance(bill.getId(), bill.getName()).show(getChildFragmentManager(), "BillDetails"); }
    
    @Override public void onActionClick(QuickLogItem item) {
        MainActivity main = (MainActivity) getActivity();
        if (main == null) return;
        String title = item.getTitle();
        if (title.equals(getString(R.string.add_options_expense))) main.navigateTo(7);
        else if (title.equals(getString(R.string.action_add_income))) main.navigateTo(8);
        else if (title.equals(getString(R.string.action_family_chat))) main.navigateTo(5);
        else if (title.equals(getString(R.string.action_manage_goals))) main.navigateTo(6);
    }

    @Override public void onQuickLogClick(QuickLogItem item) { QuickLogsBottomSheetFragment.newInstance(item.getTitle()).show(getChildFragmentManager(), "QUICK_LOG"); }
    @Override public void onQuickLogLongClick(QuickLogItem item) { startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)); }
    @Override public void onTransactionClick(TransactionWithUser item) { /* Detail */ }
    @Override public void onTransactionLike(TransactionWithUser item) { viewModel.refreshData(); }
    @Override public void onTransactionComment(TransactionWithUser item) { CommentsBottomSheetFragment.newInstance(item.getTransaction().getId()).show(getChildFragmentManager(), "Comments"); }
    @Override public void onSeeAllTransactions() { navigateTo(AllTransactionsFragment.newInstance(null)); }

    @Override 
    public void onResume() { 
        super.onResume(); 
        if (!isHidden()) {
            viewModel.refreshData(); 
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            viewModel.refreshData();
        }
    }
}
