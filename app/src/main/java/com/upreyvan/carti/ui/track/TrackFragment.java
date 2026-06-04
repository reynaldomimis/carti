package com.upreyvan.carti.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.BaseMultiItem;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.model.BudgetCategoryItem;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.ui.home.CommentsBottomSheetFragment;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class TrackFragment extends BaseFragment<com.upreyvan.carti.databinding.FragmentTrackBinding> implements TrackListItem.OnTrackInteractionListener {
    private com.upreyvan.carti.base.BaseMultiAdapter trackAdapter;
    private TrackViewModel viewModel;
    private final RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    @Override protected com.upreyvan.carti.databinding.FragmentTrackBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return com.upreyvan.carti.databinding.FragmentTrackBinding.inflate(inflater, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TrackViewModel.class);
        initAdapter();
        observeViewModel();
        viewModel.sync();
    }

    private void initAdapter() {
        trackAdapter = new com.upreyvan.carti.base.BaseMultiAdapter();
        getBinding().rvTrack.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvTrack.setAdapter(trackAdapter);
        setupSmoothScrolling(getBinding().rvTrack);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            trackAdapter.submitList(state.stream().map(this::enrich).collect(Collectors.toList()));
        });
    }

    private BaseMultiItem enrich(BaseMultiItem item) {
        if (item instanceof TrackListItem.SummaryItem s) return new TrackListItem.SummaryItem(s.balance(), s.income(), s.expense(), s.saved(), s.target(), s.progress(), this);
        if (item instanceof TrackListItem.AllocationHeaderItem a) return new TrackListItem.AllocationHeaderItem(a.totalAllocation(), a.isExpanded(), a.listSize(), this, sharedPool);
        if (item instanceof TrackListItem.SectionHeaderItem s) return new TrackListItem.SectionHeaderItem(s.title(), s.actionText(), this);
        if (item instanceof TrackListItem.TransactionItem t) return new TrackListItem.TransactionItem(t.transaction(), this);
        return item;
    }

    @Override public void onViewDetails() { navigateTo(IncomeContributorsFragment.newInstance(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.YEAR))); }
    @Override public void onTotalBalanceClick() { onViewDetails(); }
    @Override public void onToggleAllocation() { viewModel.toggleExpansion(); }
    @Override public void onSeeAllTransactions() { navigateTo(AllTransactionsFragment.newInstance("EXPENSE")); }
    @Override public void onTransactionLike(TransactionWithUser item) { viewModel.toggleLike(item); }
    @Override public void onTransactionComment(TransactionWithUser item) { CommentsBottomSheetFragment.newInstance(item.getTransaction().getId()).show(getChildFragmentManager(), "Comments"); }
    @Override public void onTransactionClick(TransactionWithUser item) {}
    
    @Override public Category findCategory(String name) {
        for (Category cat : CategoryManager.getInstance(requireContext()).getCategories()) {
            if (cat.getName().equalsIgnoreCase(name)) return cat;
        }
        return null;
    }

    @Override public List<BudgetCategoryItem> getAllocations() {
        return viewModel.getAllocations();
    }

    @Override public void onResume() { super.onResume(); if (!isHidden()) viewModel.sync(); }
}
