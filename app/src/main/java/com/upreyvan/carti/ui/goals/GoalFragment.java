package com.upreyvan.carti.ui.goals;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentGoalBinding;
import com.upreyvan.carti.databinding.ItemGoalBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.SwipeToDeleteHelper;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.List;

public class GoalFragment extends BaseFragment<FragmentGoalBinding> {

    private GenericAdapter<TransactionWithUser, ItemGoalBinding> adapter;
    private List<TransactionWithUser> allGoals = new ArrayList<>();
    private GoalViewModel viewModel;
    private RealtimeRepository realtimeRepo;
    private boolean isLoading = true;

    @Override
    protected FragmentGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GoalViewModel.class);
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        
        setupDynamicPadding();
        setupHeader();
        setupTabs();
        setupRecyclerView();
        observeViewModel();
        observeRealtimeChanges();
    }

    private void observeRealtimeChanges() {
        realtimeRepo.getGoalStream().observe(getViewLifecycleOwner(), payload -> viewModel.refresh());
    }

    private void setupDynamicPadding() {
        setupDynamicPadding(getBinding().layoutToolbar.getRoot(), getBinding().rvGoals, 0.3f);
        Utils.applySystemBarInsets(getBinding().btnAddGoalFloating, getBinding().btnAddGoalFloating, 0f, 0);
    }

    private void observeViewModel() {
        viewModel.getGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null) {
                isLoading = false;
                allGoals = goals;
                filterGoals(getBinding().tabLayout.getSelectedTabPosition());
            }
        });
        viewModel.refresh();
    }

    private void setupHeader() {
        setupToolbar(getBinding().layoutToolbar, R.string.goal_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.GONE); 
        
        getBinding().btnAddGoalFloating.setOnClickListener(v -> 
                startActivity(new Intent(requireContext(), AddGoalActivity.class)));
    }

    private void setupTabs() {
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { filterGoals(tab.getPosition()); }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterGoals(int position) {
        List<TransactionWithUser> filteredList = new ArrayList<>();
        int titleRes = R.string.no_goals_title;
        int descRes = R.string.no_goals_desc;

        switch (position) {
            case 1 -> {
                for (TransactionWithUser g : allGoals) if (!g.getTransaction().isCompleted()) filteredList.add(g);
                titleRes = R.string.no_active_goals_title;
                descRes = R.string.no_active_goals_desc;
            }
            case 2 -> {
                for (TransactionWithUser g : allGoals) if (g.getTransaction().isCompleted()) filteredList.add(g);
                titleRes = R.string.no_completed_goals_title;
                descRes = R.string.no_completed_goals_desc;
            }
            default -> filteredList.addAll(allGoals);
        }

        boolean isEmpty = filteredList.isEmpty() && !isLoading;
        getBinding().rvGoals.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        getBinding().layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        
        if (isEmpty) {
            getBinding().tvEmptyTitle.setText(titleRes);
            getBinding().tvEmptyDesc.setText(descRes);
        }

        adapter.submitList(filteredList);
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                TransactionWithUser.DIFF_CALLBACK,
                (inflater, parent) -> ItemGoalBinding.inflate(inflater, parent, false),
                (binding, itemWithUser) -> {
                    Transaction goal = itemWithUser.getTransaction();
                    View shimmer = binding.getRoot().findViewById(R.id.shimmerView);
                    if (isLoading) {
                        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
                        binding.layoutContent.setVisibility(View.INVISIBLE);
                    } else {
                        if (shimmer != null) shimmer.setVisibility(View.GONE);
                        binding.layoutContent.setVisibility(View.VISIBLE);
                        binding.tvGoalTitle.setText(goal.getName());
                        binding.ivGoalIcon.setImageResource(goal.getIconRes());
                        binding.ivGoalIcon.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant));
                        binding.tvGoalProgressAmount.setText(getString(R.string.goal_progress_amount_format,
                                Utils.formatCurrency(goal.getAmount()),
                                Utils.formatCurrency(goal.getTargetAmount())));
                        binding.progressIndicator.setProgress(goal.getProgress());
                        binding.tvPercentage.setText(getString(R.string.percentage_format, goal.getProgress()));
                        binding.tvTargetDate.setText(goal.getTargetDate() != null ? 
                                goal.getTargetDate() : getString(R.string.label_days_left, 0));
                    }
                }
        );
        adapter.setOnItemClickListener(item -> navigateTo(GoalDetailFragment.newInstance(item.getTransaction().getId())));
        adapter.setOnItemLongClickListener(item -> {
            UpdateGoalBottomSheetFragment.newInstance(item.getTransaction().getId())
                    .show(getChildFragmentManager(), "UPDATE_GOAL");
            return true;
        });
        
        getBinding().rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvGoals.setAdapter(adapter);

        SwipeToDeleteHelper.attach(getBinding().rvGoals, position -> {
            TransactionWithUser goalToDelete = adapter.getItem(position);
            List<TransactionWithUser> currentList = new ArrayList<>(adapter.getCurrentList());
            currentList.remove(position);
            adapter.submitList(currentList);

            String message = String.format("Goal '%s' deleted", goalToDelete.getTransaction().getName());
            SwipeToDeleteHelper.showUndoSnackbar(getBinding().getRoot(), message, () -> {
                List<TransactionWithUser> restoredList = new ArrayList<>(adapter.getCurrentList());
                restoredList.add(position, goalToDelete);
                adapter.submitList(restoredList);
            }, () -> viewModel.deleteGoal(goalToDelete.getTransaction().getId()));
        });
    }
}
