package com.upreyvan.carti.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.BaseMultiItem;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.managers.SalaryManager;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.ui.bills.BillDetailsBottomSheet;
import com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment;
import com.upreyvan.carti.ui.goals.UpdateGoalBottomSheetFragment;
import com.upreyvan.carti.ui.track.AddBudgetPlanActivity;
import com.upreyvan.carti.ui.track.AllTransactionsFragment;
import com.upreyvan.carti.ui.track.ExpenseEditBottomSheet;
import com.upreyvan.carti.ui.track.IncomeEditBottomSheet;
import com.upreyvan.carti.utils.Constants;
import com.upreyvan.carti.utils.DialogHelper;
import com.upreyvan.carti.utils.UiHelper;
import com.upreyvan.carti.utils.Utils;

import java.util.Objects;
import java.util.stream.Collectors;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> implements HomeListItem.OnHomeInteractionListener {
    private com.upreyvan.carti.base.BaseMultiAdapter homeAdapter;
    private HomeViewModel viewModel;
    private final RecyclerView.RecycledViewPool billPool = new RecyclerView.RecycledViewPool();
    private final RecyclerView.RecycledViewPool actionPool = new RecyclerView.RecycledViewPool();
    private final RecyclerView.RecycledViewPool logPool = new RecyclerView.RecycledViewPool();

    @Override protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        if (getActivity() instanceof MainActivity main) main.setBottomNavVisibility(true);
        setupHeaders();
        initAdapter();
        observeViewModel();
        setupListeners();
        observeNotifications();
    }

    private void setupListeners() {
        getBinding().btnNotif.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity main) {
                main.navigateTo(Constants.Navigation.NOTIFICATIONS);
            }
        });
    }

    private void observeNotifications() {
        com.upreyvan.carti.repository.NotificationRepository.getInstance(requireContext())
                .getUnreadCount().observe(getViewLifecycleOwner(), count -> {
                    boolean hasUnread = count != null && count > 0;
                    getBinding().notifBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), 
                            hasUnread ? R.color.carti_primary_green : R.color.nav_inactive)
                    ));
                    // Keep it visible as gray if read, or you can still hide it if preferred.
                    // The user asked for gray when read.
                    getBinding().notifBadge.setVisibility(View.VISIBLE);
                });
    }

    private void initAdapter() {
        homeAdapter = new com.upreyvan.carti.base.BaseMultiAdapter();
        RecyclerView rv = getBinding().rvMainHome;
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(homeAdapter);
        rv.setItemViewCacheSize(10);
        rv.setHasFixedSize(true);
        setupSmoothScrolling(rv);

        getBinding().swipeRefresh.setOnRefreshListener(() -> viewModel.refreshData());
        getBinding().swipeRefresh.setColorSchemeResources(R.color.carti_primary_green);
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            getBinding().swipeRefresh.setRefreshing(loading);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            homeAdapter.submitList(state.stream().map(this::enrich).collect(Collectors.toList()));
        });
    }

    private BaseMultiItem enrich(BaseMultiItem item) {
        if (item instanceof HomeListItem.BudgetPromptItem) return new HomeListItem.BudgetPromptItem(this::onBudgetPromptClick);
        if (item instanceof HomeListItem.BillContainerItem b) return new HomeListItem.BillContainerItem(b.bills(), this, billPool);
        if (item instanceof HomeListItem.QuickActionsItem q) return new HomeListItem.QuickActionsItem(q.actions(), this, actionPool);
        if (item instanceof HomeListItem.QuickLogItemContainer l) return new HomeListItem.QuickLogItemContainer(l.logs(), this, logPool);
        if (item instanceof HomeListItem.TransactionItem t) return new HomeListItem.TransactionItem(t.transaction(), this);
        if (item instanceof HomeListItem.SectionHeaderItem s) {
            if (getString(R.string.recent_activity).equals(s.title())) return new HomeListItem.SectionHeaderItem(s.title(), s.subtitle(), s.showAction(), s.actionText(), this::onSeeAllTransactions);
        }
        return item;
    }

    private void setupHeaders() {
        PreferenceManager pref = PreferenceManager.getInstance(requireContext());
        getBinding().tvGreetingMain.setText(getString(R.string.format_greeting, Utils.getGreeting()));
        getBinding().tvUsernameMain.setText(getString(R.string.format_username, pref.getUsername()));
        SalaryManager sm = SalaryManager.getInstance(requireContext());
        getBinding().tvGreetingSub.setText(String.format("%s • %s", getString(R.string.days_to_go, sm.getDaysUntilNextPayday()), Utils.formatDateShort(sm.getNextPayday())));
    }

    @Override public void onBudgetPromptClick() { startActivity(new Intent(requireContext(), AddBudgetPlanActivity.class)); }
    @Override public void onBillClick(Bill bill) { BillDetailsBottomSheet.newInstance(bill.getId(), bill.getName()).show(getChildFragmentManager(), "BillDetails"); }
    @Override public void onActionClick(com.upreyvan.carti.models.QuickLogItem item) {
        MainActivity main = (MainActivity) getActivity(); if (main == null) return;
        String t = item.getTitle();
        if (Objects.equals(t, getString(R.string.add_options_expense))) {
            main.navigateTo(Constants.Navigation.TRACK);
        } else if (Objects.equals(t, getString(R.string.action_add_income))) {
            main.navigateTo(Constants.Navigation.TRACK);
        } else if (Objects.equals(t, getString(R.string.action_family_chat))) {
            main.navigateTo(Constants.Navigation.CHAT);
        } else if (Objects.equals(t, getString(R.string.action_manage_goals))) {
            main.navigateTo(Constants.Navigation.PLAN);
        }
    }
    @Override public void onQuickLogClick(com.upreyvan.carti.models.QuickLogItem item) { QuickLogsBottomSheetFragment.newInstance(item.getTitle()).show(getChildFragmentManager(), "QUICK_LOG"); }
    @Override public void onQuickLogLongClick(com.upreyvan.carti.models.QuickLogItem item) { startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)); }
    @Override public void onTransactionClick(TransactionWithUser item) {}
    @Override public void onTransactionLike(TransactionWithUser item) { viewModel.toggleLike(item); }
    @Override public void onTransactionReaction(TransactionWithUser item, String emoji) { viewModel.toggleReaction(item, emoji); }
    @Override public void onTransactionComment(TransactionWithUser item) { CommentsBottomSheetFragment.newInstance(item.getTransaction().getId()).show(getChildFragmentManager(), "Comments"); }
    @Override public void onViewLikes(TransactionWithUser item) { ReactionsBottomSheetFragment.newInstance(item.getTransaction().getId()).show(getChildFragmentManager(), "Reactions"); }
    @Override public void onSeeAllTransactions() { navigateTo(AllTransactionsFragment.newInstance(null)); }
    @Override public void onTransactionEdit(TransactionWithUser item) {
        Transaction t = item.getTransaction();
        String type = t.getType();
        if ("INCOME".equals(type)) {
            IncomeEditBottomSheet.newInstance(t).show(getChildFragmentManager(), "EditIncome");
        } else if ("GOAL".equals(type)) {
            UpdateGoalBottomSheetFragment.newInstance(t.getId()).show(getChildFragmentManager(), "EditGoal");
        } else if ("EXPENSE".equals(type) || "BILL".equals(type) || "DEBT".equals(type)) {
            ExpenseEditBottomSheet.newInstance(t).show(getChildFragmentManager(), "EditExpense");
        } else {
            showToast("Edit for " + type + " coming soon", UiHelper.Status.INFO);
        }
    }
    @Override public void onTransactionDelete(TransactionWithUser item) {
        DialogHelper.showConfirmation(requireContext(), 
            "Delete Transaction?", 
            "Are you sure you want to delete this " + (item.getTransaction().getTitle() != null ? item.getTransaction().getTitle() : "transaction") + "?", 
            "Delete", () -> {
            viewModel.deleteTransaction(item);
            showToast(R.string.msg_deleted_balance_updated, UiHelper.Status.SUCCESS);
        });
    }
    @Override public String getCurrentUserId() {
        return PreferenceManager.getInstance(requireContext()).getUserId();
    }
    @Override public void onResume() { super.onResume(); if (!isHidden()) viewModel.refreshData(); }
}
