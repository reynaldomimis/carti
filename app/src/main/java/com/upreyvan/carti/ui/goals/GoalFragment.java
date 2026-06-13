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
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentGoalBinding;
import com.upreyvan.carti.databinding.ItemGoalBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.ui.allocate.PlanViewModel;
import com.upreyvan.carti.utils.SwipeToDeleteHelper;
import com.upreyvan.carti.utils.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GoalFragment extends BaseFragment<FragmentGoalBinding> {

    private GenericAdapter<TransactionWithUser, ItemGoalBinding> adapter;
    private List<TransactionWithUser> allGoals = new ArrayList<>();
    private PlanViewModel viewModel;
    private RealtimeRepository realtimeRepo;
    private boolean isLoading = true;

    @Override
    protected FragmentGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlanViewModel.class);
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        
        setupUI();
        setupTabs();
        setupRecyclerView();
        setupDynamicPadding(null, getBinding().getRoot());
        observeViewModel();
        observeRealtimeChanges();
    }

    private void setupUI() {
        getBinding().layoutHeader.tvHeaderTitle.setText(R.string.label_setup_goals);
        getBinding().layoutHeader.tvHeaderSubtitle.setText(Utils.formatMonthYear(Calendar.getInstance()));
        getBinding().layoutHeader.btnHeaderAction.setText(R.string.btn_add_goal);
        getBinding().layoutHeader.btnHeaderAction.setOnClickListener(v -> 
                AddGoalBottomSheetFragment.newInstance().show(getChildFragmentManager(), "ADD_GOAL"));
    }

    private void observeRealtimeChanges() {
        realtimeRepo.getGoalStream().observe(getViewLifecycleOwner(), payload -> viewModel.refresh());
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

    private void setupTabs() {
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { filterGoals(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterGoals(int position) {
        List<TransactionWithUser> filteredList = new ArrayList<>();
        int titleRes;
        int descRes;

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
            default -> {
                filteredList.addAll(allGoals);
                titleRes = R.string.no_goals_title;
                descRes = R.string.no_goals_desc;
            }
        }
        
        Utils.sortAlphabetically(filteredList, g -> g.getTransaction().getTitle());

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
                (binding, itemWithUser, position, totalCount) -> {
                    Transaction goal = itemWithUser.getTransaction();
                    View shimmer = binding.shimmerView.getRoot();
                    if (isLoading) {
                        shimmer.setVisibility(View.VISIBLE);
                        binding.layoutContent.setVisibility(View.INVISIBLE);
                    } else {
                        shimmer.setVisibility(View.GONE);
                        binding.layoutContent.setVisibility(View.VISIBLE);
                        binding.tvGoalTitle.setText(goal.getTitle());
                        
                        // Centralized Style Implementation
                        binding.ivGoalIcon.setImageResource(goal.getIconRes() != 0 ? goal.getIconRes() : R.drawable.ic_trophy);
                        binding.ivGoalIcon.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), goal.getIconBgColor()));
                        binding.ivGoalIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), goal.getIconColor()));
                        
                        if (goal.getIconUrl() != null && !goal.getIconUrl().isEmpty()) {
                            com.bumptech.glide.Glide.with(requireContext()).load(goal.getIconUrl()).into(binding.ivGoalIcon);
                            binding.ivGoalIcon.setImageTintList(null);
                        }

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

        SwipeToDeleteHelper.attach(getBinding().rvGoals, pos -> {
            TransactionWithUser goalToDelete = adapter.getItem(pos);
            List<TransactionWithUser> currentList = new ArrayList<>(adapter.getCurrentList());
            currentList.remove(pos);
            adapter.submitList(currentList);

            String msg = String.format("Goal '%s' deleted", goalToDelete.getTransaction().getTitle());
            SwipeToDeleteHelper.showUndoSnackbar(getBinding().getRoot(), msg, () -> {
                List<TransactionWithUser> restoredList = new ArrayList<>(adapter.getCurrentList());
                restoredList.add(pos, goalToDelete);
                adapter.submitList(restoredList);
            }, () -> viewModel.deleteGoal(goalToDelete.getTransaction().getId()));
        });
    }
}
