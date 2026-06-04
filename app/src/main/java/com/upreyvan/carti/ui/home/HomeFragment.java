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
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.SalaryManager;
import com.upreyvan.carti.databinding.FragmentHomeBinding;
import com.upreyvan.carti.ui.bills.BillDetailsBottomSheet;
import com.upreyvan.carti.ui.common.QuickLogsBottomSheetFragment;
import com.upreyvan.carti.ui.track.AddBudgetPlanActivity;
import com.upreyvan.carti.ui.track.AllTransactionsFragment;
import com.upreyvan.carti.util.Utils;

import java.util.stream.Collectors;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> implements HomeListItem.OnHomeInteractionListener {
    private com.upreyvan.carti.base.BaseMultiAdapter homeAdapter;
    private HomeViewModel viewModel;
    private final RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

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
    }

    private void initAdapter() {
        homeAdapter = new com.upreyvan.carti.base.BaseMultiAdapter();
        getBinding().rvMainHome.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMainHome.setAdapter(homeAdapter);
        setupSmoothScrolling(getBinding().rvMainHome);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            homeAdapter.submitList(state.stream().map(this::enrich).collect(Collectors.toList()));
        });
    }

    private BaseMultiItem enrich(BaseMultiItem item) {
        if (item instanceof HomeListItem.BudgetPromptItem) return new HomeListItem.BudgetPromptItem(this::onBudgetPromptClick);
        if (item instanceof HomeListItem.BillContainerItem b) return new HomeListItem.BillContainerItem(b.bills(), this, sharedPool);
        if (item instanceof HomeListItem.QuickActionsItem q) return new HomeListItem.QuickActionsItem(q.actions(), this, sharedPool);
        if (item instanceof HomeListItem.QuickLogItemContainer l) return new HomeListItem.QuickLogItemContainer(l.logs(), this, sharedPool);
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
    @Override public void onBillClick(com.upreyvan.carti.model.Bill bill) { BillDetailsBottomSheet.newInstance(bill.getId(), bill.getName()).show(getChildFragmentManager(), "BillDetails"); }
    @Override public void onActionClick(com.upreyvan.carti.model.QuickLogItem item) {
        MainActivity main = (MainActivity) getActivity(); if (main == null) return;
        String t = item.getTitle();
        if (t.equals(getString(R.string.add_options_expense))) main.navigateTo(com.upreyvan.carti.util.Constants.Navigation.TRACK);
        else if (t.equals(getString(R.string.action_add_income))) main.navigateTo(com.upreyvan.carti.util.Constants.Navigation.TRACK);
        else if (t.equals(getString(R.string.action_family_chat))) main.navigateTo(com.upreyvan.carti.util.Constants.Navigation.CHAT);
        else if (t.equals(getString(R.string.action_manage_goals))) main.navigateTo(com.upreyvan.carti.util.Constants.Navigation.PLAN);
    }
    @Override public void onQuickLogClick(com.upreyvan.carti.model.QuickLogItem item) { QuickLogsBottomSheetFragment.newInstance(item.getTitle()).show(getChildFragmentManager(), "QUICK_LOG"); }
    @Override public void onQuickLogLongClick(com.upreyvan.carti.model.QuickLogItem item) { startActivity(new Intent(requireContext(), CustomizeQuickLogActivity.class)); }
    @Override public void onTransactionClick(com.upreyvan.carti.model.TransactionWithUser item) {}
    @Override public void onTransactionLike(com.upreyvan.carti.model.TransactionWithUser item) { viewModel.toggleLike(item); }
    @Override public void onTransactionComment(com.upreyvan.carti.model.TransactionWithUser item) { com.upreyvan.carti.ui.home.CommentsBottomSheetFragment.newInstance(item.getTransaction().getId()).show(getChildFragmentManager(), "Comments"); }
    @Override public void onSeeAllTransactions() { navigateTo(AllTransactionsFragment.newInstance(null)); }
    @Override public void onResume() { super.onResume(); if (!isHidden()) viewModel.refreshData(); }
}
