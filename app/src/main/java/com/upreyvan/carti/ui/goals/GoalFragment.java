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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        
        setupDynamicPadding(getBinding().layoutToolbar.getRoot(), getBinding().rvGoals, 0.3f);
        setupHeader();
        setupTabs();
        setupRecyclerView();
        observeGoals();
    }

    private void observeGoals() {
        goalRepository.getAllGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null) {
                isLoading = goals.isEmpty();
                allGoals = goals;
                if (isLoading) {
                    List<Goal> placeholders = new ArrayList<>();
                    for (int i = 0; i < 3; i++) placeholders.add(new Goal());
                    adapter.submitList(placeholders);
                } else {
                    filterGoals(getBinding().tabLayout.getSelectedTabPosition());
                    updateOverallProgress(allGoals);
                }
            }
        });
        goalRepository.syncGoalsIfNeeded();
    }

    private void setupHeader() {
        setupToolbar(getBinding().layoutToolbar, R.string.goal_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_add_goal);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
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
        if (position == 0) {
            for (Goal goal : allGoals) {
                if (!goal.isCompleted()) filteredList.add(goal);
            }
        } else {
            for (Goal goal : allGoals) {
                if (goal.isCompleted()) filteredList.add(goal);
            }
        }
        adapter.submitList(filteredList);
    }

    private void updateOverallProgress(List<Goal> goals) {
        double totalCurrent = 0;
        double totalTarget = 0;

        for (Goal goal : goals) {
            totalCurrent += goal.getCurrentAmount();
            totalTarget += goal.getTargetAmount();
        }

        getBinding().tvTotalAmount.setText(Utils.formatCurrency(totalCurrent));

        if (totalTarget > 0) {
            int progress = (int) ((totalCurrent / totalTarget) * 100);
            getBinding().tvOverallPercentage.setText(getString(R.string.percentage_format, progress));
        } else {
            getBinding().tvOverallPercentage.setText(getString(R.string.zero_percent));
        }
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
                        binding.ivGoalIcon.setBackgroundColor(goal.getBackgroundColor());
                        
                        String progressText = String.format(Locale.getDefault(), 
                                getString(R.string.goal_progress_amount_format),
                                Utils.formatCurrency(goal.getCurrentAmount()),
                                Utils.formatCurrency(goal.getTargetAmount()));
                        
                        binding.tvGoalProgressAmount.setText(progressText);
                        binding.progressIndicator.setProgress(goal.getProgress());
                        binding.tvPercentage.setText(getString(R.string.percentage_format, goal.getProgress()));
                        binding.tvTargetDate.setText(getString(R.string.target_date_label_format, goal.getTargetDate()));
                    }
                }
        );
        adapter.setOnItemClickListener(goal -> navigateTo(GoalDetailFragment.newInstance(goal.getId())));
        getBinding().rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvGoals.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (goalRepository != null) {
            goalRepository.onDestroy();
        }
    }
}
