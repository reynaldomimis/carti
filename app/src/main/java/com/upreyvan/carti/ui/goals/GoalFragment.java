package com.upreyvan.carti.ui.goals;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.GoalRepository;
import com.upreyvan.carti.databinding.FragmentGoalBinding;
import com.upreyvan.carti.databinding.ItemGoalBinding;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.util.Utils;

import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.util.SwipeToDeleteHelper;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class GoalFragment extends BaseFragment<FragmentGoalBinding> {

    private GenericAdapter<Goal, ItemGoalBinding> adapter;
    private List<Goal> allGoals = new ArrayList<>();
    private GoalRepository goalRepository;
    private boolean isLoading = true;

    @Override
    protected FragmentGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        goalRepository = new GoalRepository(requireContext());
        
        setupDynamicPadding();
        setupHeader();
        setupTabs();
        setupRecyclerView();
        observeGoals();
    }

    private void setupDynamicPadding() {
       setupDynamicPadding(getBinding().layoutToolbar.getRoot(), getBinding().rvGoals, 0.3f);
        com.upreyvan.carti.util.Utils.applySystemBarInsets(
                getBinding().btnAddGoalFloating,
                getBinding().btnAddGoalFloating,
                0f,
                0
        );
    }

    private void observeGoals() {
        goalRepository.getAllGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null) {
                isLoading = false;
                allGoals = goals;
                filterGoals(getBinding().tabLayout.getSelectedTabPosition());
            }
        });
        goalRepository.syncGoalsIfNeeded();
    }

    private void setupHeader() {
        setupToolbar(getBinding().layoutToolbar, R.string.goal_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.GONE); // Moved to bottom
        
        getBinding().btnAddGoalFloating.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddGoalActivity.class));
        });
    }

    private void setupTabs() {
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterGoals(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterGoals(int position) {
        List<Goal> filteredList = new ArrayList<>();
        String emptyTitle;
        String emptyDesc;

        switch (position) {
            case 0:
                filteredList.addAll(allGoals);
                emptyTitle = getString(R.string.no_goals_title);
                emptyDesc = getString(R.string.no_goals_desc);
                break;
            case 1:
                for (Goal goal : allGoals) {
                    if (!goal.isCompleted()) filteredList.add(goal);
                }
                emptyTitle = getString(R.string.no_active_goals_title);
                emptyDesc = getString(R.string.no_active_goals_desc);
                break;
            case 2:
                for (Goal goal : allGoals) {
                    if (goal.isCompleted()) filteredList.add(goal);
                }
                emptyTitle = getString(R.string.no_completed_goals_title);
                emptyDesc = getString(R.string.no_completed_goals_desc);
                break;
            case 3:
                emptyTitle = getString(R.string.no_paused_goals_title);
                emptyDesc = getString(R.string.no_paused_goals_desc);
                break;
            default:
                emptyTitle = getString(R.string.no_goals_title);
                emptyDesc = getString(R.string.no_goals_desc);
                break;
        }

        if (filteredList.isEmpty() && !isLoading) {
            getBinding().rvGoals.setVisibility(View.GONE);
            getBinding().layoutEmptyState.setVisibility(View.VISIBLE);
            getBinding().tvEmptyTitle.setText(emptyTitle);
            getBinding().tvEmptyDesc.setText(emptyDesc);
        } else {
            getBinding().rvGoals.setVisibility(View.VISIBLE);
            getBinding().layoutEmptyState.setVisibility(View.GONE);
        }

        adapter.submitList(filteredList);
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                Goal.DIFF_CALLBACK,
                (inflater, parent) -> ItemGoalBinding.inflate(inflater, parent, false),
                (binding, goal) -> {
                    View shimmer = binding.getRoot().findViewById(R.id.shimmerView);
                    if (isLoading) {
                        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
                        binding.layoutContent.setVisibility(View.INVISIBLE);
                    } else {
                        if (shimmer != null) shimmer.setVisibility(View.GONE);
                        binding.layoutContent.setVisibility(View.VISIBLE);
                        
                        binding.tvGoalTitle.setText(goal.getTitle());
                        binding.ivGoalIcon.setImageResource(goal.getImageRes());
                        binding.ivGoalIcon.setBackgroundColor(requireContext().getColor(R.color.surface_variant));
                        
                        String progressText = getString(R.string.goal_progress_amount_format,
                                Utils.formatCurrency(goal.getCurrentAmount()),
                                Utils.formatCurrency(goal.getTargetAmount()));
                        
                        binding.tvGoalProgressAmount.setText(progressText);
                        binding.progressIndicator.setProgress(goal.getProgress());
                        binding.tvPercentage.setText(getString(R.string.percentage_format, goal.getProgress()));
                        
                        // Mocking days left for now if not available in model
                        binding.tvTargetDate.setText(getString(R.string.label_days_left, 60));
                    }
                }
        );
        adapter.setOnItemClickListener(goal -> navigateTo(GoalDetailFragment.newInstance(goal.getId())));
        adapter.setOnItemLongClickListener(goal -> {
            UpdateGoalBottomSheetFragment bottomSheet = UpdateGoalBottomSheetFragment.newInstance(goal.getId());
            bottomSheet.show(getChildFragmentManager(), "UPDATE_GOAL");
            return true;
        });
        getBinding().rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvGoals.setAdapter(adapter);

        SwipeToDeleteHelper.attach(getBinding().rvGoals, position -> {
            Goal goalToDelete = adapter.getItem(position);
            List<Goal> currentList = new ArrayList<>(adapter.getCurrentList());
            currentList.remove(position);
            adapter.submitList(currentList);

            SwipeToDeleteHelper.showUndoSnackbar(getBinding().getRoot(), "Goal '" + goalToDelete.getTitle() + "' deleted", () -> {
                // UNDO
                List<Goal> restoredList = new ArrayList<>(adapter.getCurrentList());
                restoredList.add(position, goalToDelete);
                adapter.submitList(restoredList);
            }, () -> {
                // ACTUAL DELETE
                goalRepository.deleteGoal(goalToDelete.getId(), new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        // Successfully deleted remotely and locally (handled by repo)
                    }

                    @Override
                    public void onError(Throwable error) {
                        // Error handling could be added here
                    }
                });
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (goalRepository != null) {
            goalRepository.onDestroy();
        }
    }
}
