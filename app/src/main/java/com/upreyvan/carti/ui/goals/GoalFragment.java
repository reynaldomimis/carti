package com.upreyvan.carti.ui.goals;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentGoalBinding;
import com.upreyvan.carti.databinding.ItemGoalBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.ui.allocate.PlanViewModel;
import com.upreyvan.carti.utils.DateHelper;
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
    private PreferenceManager pref;
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
        pref = PreferenceManager.getInstance(requireContext());
        
        setupUI();
        setupTabs();
        setupRecyclerView();
        setupDynamicPadding(null, getBinding().getRoot());
        observeViewModel();
        observeRealtimeChanges();
    }

    private void setupUI() {
        getBinding().layoutHeader.tvHeaderTitle.setText(R.string.label_setup_goals);
        getBinding().layoutHeader.tvHeaderSubtitle.setText("Plan your vision. Secure your future.");
        getBinding().layoutHeader.btnHeaderAction.setText(R.string.btn_add_goal);
        getBinding().layoutHeader.btnHeaderAction.setOnClickListener(v -> AddGoalBottomSheetFragment.newInstance().show(getChildFragmentManager(), "ADD_GOAL"));
    }

    private void observeRealtimeChanges() {
        realtimeRepo.getGoalStream().observe(getViewLifecycleOwner(), payload -> viewModel.refresh());
    }

    private void observeViewModel() {
        viewModel.getGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null) {
                if (isLoading) {
                    getBinding().shimmerGoals.stopShimmer();
                    getBinding().shimmerGoals.setVisibility(View.GONE);
                    getBinding().rvGoals.setVisibility(View.VISIBLE);
                    isLoading = false;
                }
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

        List<TransactionWithUser> mainGoals = new ArrayList<>();
        for (TransactionWithUser tu : allGoals) {
            if (tu.getTransaction().getTargetAmount() > 0) {
                mainGoals.add(tu);
            }
        }

        switch (position) {
            case 1 -> { // Active
                for (TransactionWithUser g : mainGoals) {
                    if ("ACTIVE".equalsIgnoreCase(g.getTransaction().getStatus())) filteredList.add(g);
                }
                titleRes = R.string.no_active_goals_title;
                descRes = R.string.no_active_goals_desc;
            }
            case 2 -> { // Completed
                for (TransactionWithUser g : mainGoals) {
                    if ("COMPLETED".equalsIgnoreCase(g.getTransaction().getStatus())) filteredList.add(g);
                }
                titleRes = R.string.no_completed_goals_title;
                descRes = R.string.no_completed_goals_desc;
            }
            case 3 -> { // Canceled
                for (TransactionWithUser g : mainGoals) {
                    if ("CANCELED".equalsIgnoreCase(g.getTransaction().getStatus()) || "CANCELLED".equalsIgnoreCase(g.getTransaction().getStatus())) filteredList.add(g);
                }
                titleRes = R.string.no_canceled_goals_title;
                descRes = R.string.no_canceled_goals_desc;
            }
            default -> {
                filteredList.addAll(mainGoals);
                titleRes = R.string.no_goals_title;
                descRes = R.string.no_goals_desc;
            }
        }
        
        // Sort by timestamp descending (Latest first)
        filteredList.sort((g1, g2) -> Long.compare(g2.getTransaction().getTimestampMillis(), g1.getTransaction().getTimestampMillis()));

        boolean isEmpty = filteredList.isEmpty() && !isLoading;
        getBinding().rvGoals.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        getBinding().layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        
        if (isEmpty) {
            getBinding().tvEmptyTitle.setText(titleRes);
            getBinding().tvEmptyDesc.setText(descRes);
        }

        adapter.submitList(filteredList);
    }

    private double calculateGoalSum(Transaction goal) {
        double total = 0;
        for (TransactionWithUser tu : allGoals) {
            Transaction t = tu.getTransaction();
            // UNIQUE LINK: Sum by allocatedTo ID primarily, fallback to Title only for legacy data
            if ("GOAL".equalsIgnoreCase(t.getType()) && t.getTargetAmount() <= 0) {
                if (goal.getId().equals(t.getAllocatedTo()) || goal.getTitle().equalsIgnoreCase(t.getCategory())) {
                    total += t.getAmount();
                }
            }
        }
        return total;
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
                        
                        // Centralized Style Implementation with safety fallback
                        int iconRes = goal.getIconRes() != 0 ? goal.getIconRes() : R.drawable.ic_trophy;
                        int bgColorRes = goal.getIconBgColor() != 0 ? goal.getIconBgColor() : R.color.mint_green_alpha;
                        int iconColorRes = goal.getIconColor() != 0 ? goal.getIconColor() : R.color.carti_primary_green;

                        binding.ivGoalIcon.setImageResource(iconRes);
                        binding.ivGoalIcon.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), bgColorRes));
                        binding.ivGoalIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), iconColorRes));
                        
                        if (goal.getIconUrl() != null && !goal.getIconUrl().isEmpty()) {
                            com.bumptech.glide.Glide.with(requireContext()).load(goal.getIconUrl()).into(binding.ivGoalIcon);
                            binding.ivGoalIcon.setImageTintList(null);
                        }

                        // Use dynamic calculation for the numerator (sum of all related transactions)
                        double currentProgressAmount = calculateGoalSum(goal);
                        double targetGoalAmount = goal.getTargetAmount();

                        // Progress Calculation
                        int progress = (int) Math.min(100, (currentProgressAmount / targetGoalAmount) * 100);

                        binding.tvGoalProgressAmount.setText(getString(R.string.goal_progress_amount_format,
                                Utils.formatCurrency(currentProgressAmount),
                                Utils.formatCurrency(targetGoalAmount)));
                        
                        binding.progressIndicator.setProgress(progress);
                        binding.tvPercentage.setText(getString(R.string.percentage_format, progress));
                        
                        // NEW TARGET DATE & STATUS UX
                        binding.tvTargetDate.setText(com.upreyvan.carti.utils.GoalHelper.getGoalStatusLabel(requireContext(), goal));
                        binding.tvTargetDate.setTextColor(com.upreyvan.carti.utils.GoalHelper.getLabelColor(requireContext(), goal));

                        // DYNAMIC BUTTON BEHAVIOR: OWNER VS NON-OWNER RULES
                        String statusStr = goal.getStatus() != null ? goal.getStatus().toUpperCase() : "ACTIVE";
                        String currentUserId = pref.getUserId();
                        
                        // Robust Ownership Check: Handling possible mapping mismatches or nulls
                        boolean isOwner = false;
                        if (currentUserId != null && !currentUserId.isEmpty()) {
                            isOwner = currentUserId.equals(goal.getUserId()) || 
                                     currentUserId.equals(itemWithUser.getTransaction().getUserId());
                        }

                        // Reset button state
                        binding.btnContribute.setVisibility(View.VISIBLE);
                        binding.btnContribute.setEnabled(true);
                        binding.btnContribute.setClickable(true);
                        binding.btnContribute.setElevation(0);
                        binding.btnContribute.setStrokeWidth(0);
                        binding.btnContribute.setPadding(com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), 12), 0, com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), 12), 0);
                        binding.btnContribute.setTypeface(null, android.graphics.Typeface.NORMAL);

                        if ("ACTIVE".equals(statusStr)) {
                            if (isOwner) {
                                // OWNER FLOW
                                if (com.upreyvan.carti.utils.GoalHelper.isTargetReached(goal)) {
                                    // REACHED -> Check Status (Red)
                                    binding.btnContribute.setText("Check Status");
                                    binding.btnContribute.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_red_tonal));
                                    binding.btnContribute.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
                                    binding.btnContribute.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.status_red));
                                    binding.btnContribute.setStrokeWidth(com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), 1));

                                    binding.btnContribute.setOnClickListener(v -> 
                                            GoalStatusBottomSheetFragment.newInstance(goal, true).show(getChildFragmentManager(), "GOAL_STATUS_MANAGE"));
                                } else {
                                    // NOT REACHED -> Action (Green)
                                    binding.btnContribute.setText("Action");
                                    binding.btnContribute.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.mint_green_alpha));
                                    binding.btnContribute.setTextColor(ContextCompat.getColor(requireContext(), R.color.carti_primary_green));

                                    binding.btnContribute.setOnClickListener(v -> 
                                            GoalStatusBottomSheetFragment.newInstance(goal, false).show(getChildFragmentManager(), "GOAL_ACTIONS"));
                                }
                            } else {
                                // NON-OWNER FLOW -> Always directly "Contribute"
                                binding.btnContribute.setText("Contribute");
                                binding.btnContribute.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.mint_green_alpha));
                                binding.btnContribute.setTextColor(ContextCompat.getColor(requireContext(), R.color.carti_primary_green));

                                binding.btnContribute.setOnClickListener(v -> 
                                        GoalContributeBottomSheetFragment.newInstance(goal).show(getChildFragmentManager(), "CONTRIBUTE_GOAL"));
                            }
                        } else {
                            // COMPLETED or CANCELED: Hide action buttons for everyone
                            binding.btnContribute.setVisibility(View.GONE);
                            binding.btnContribute.setOnClickListener(null);
                        }

                        // Contributor Avatar Stack logic
                        binding.avatarStack.removeAllViews();
                        java.util.Map<String, String> uniqueContributors = new java.util.LinkedHashMap<>();
                        for (TransactionWithUser tu : allGoals) {
                            Transaction contribution = tu.getTransaction();
                            if ("GOAL".equalsIgnoreCase(contribution.getType()) && 
                                contribution.getTargetAmount() <= 0 &&
                                (goal.getId().equals(contribution.getAllocatedTo()) || 
                                 goal.getTitle().equalsIgnoreCase(contribution.getCategory()))) {

                                String userId = contribution.getUserId();
                                String userName = tu.getUsername() != null ? tu.getUsername() : contribution.getUsername();
                                if (userId != null) {
                                    uniqueContributors.put(userId, userName);
                                }
                            }
                        }

                        List<String> contributorIds = new ArrayList<>(uniqueContributors.keySet());
                        int displayCount = Math.min(contributorIds.size(), 3);
                        for (int idx = 0; idx < displayCount; idx++) {
                            com.google.android.material.imageview.ShapeableImageView iv = new com.google.android.material.imageview.ShapeableImageView(requireContext());
                            int avatarSize = com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), 24);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
                            if (idx > 0) lp.setMarginStart(com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), -8));
                            iv.setLayoutParams(lp);
                            iv.setShapeAppearanceModel(iv.getShapeAppearanceModel().toBuilder()
                                    .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, (float) avatarSize / 2)
                                    .build());
                            iv.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                            iv.setStrokeWidth((float) com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), 1));
                            
                            String userId = contributorIds.get(idx);
                            String userName = uniqueContributors.get(userId);
                            com.upreyvan.carti.utils.AvatarHelper.loadUserAvatar(requireContext(), iv, userName);
                            binding.avatarStack.addView(iv);
                        }

                        if (contributorIds.size() > 3) {
                            TextView tvPlus = new TextView(requireContext());
                            tvPlus.setText(String.format(java.util.Locale.getDefault(), "+%d", contributorIds.size() - 3));
                            tvPlus.setTextSize(10);
                            tvPlus.setPadding(com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), 4), 0, 0, 0);
                            tvPlus.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            binding.avatarStack.addView(tvPlus);
                        }
                        
                        binding.avatarStack.setVisibility(contributorIds.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }
        );
        // Removed item click and long click listeners to prevent redundant navigation and bottom sheets
        adapter.setOnItemClickListener(null);
        adapter.setOnItemLongClickListener(null);
        
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
            }, () -> viewModel.deleteGoal(goalToDelete.getTransaction()));
        });
    }
}
