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
import com.upreyvan.carti.data.local.GoalManager;
import com.upreyvan.carti.databinding.FragmentGoalBinding;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.util.Utils;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import java.util.Map;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoalFragment extends BaseFragment<FragmentGoalBinding> {

    private GoalAdapter adapter;
    private List<Goal> allGoals = new ArrayList<>();

    @Override
    protected FragmentGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupHeader();
        setupTabs();
        setupRecyclerView();
        loadGoals();

        GoalManager.getInstance().setOnGoalChangeListener(() -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(this::loadGoals);
            }
        });
    }

    private void loadGoals() {
        // Load from local first for instant UI
        allGoals = GoalManager.getInstance().getGoals();
        if (allGoals.isEmpty()) {
            adapter.setLoading(true);
        } else {
            filterGoals(getBinding().tabLayout.getSelectedTabPosition());
            updateOverallProgress(allGoals);
        }

        // Then fetch from Cloud (SDK Direct Read)
        new ApiHelper(requireContext()).getGoals(new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (!isAdded()) return;
                
                List<Goal> cloudGoals = new ArrayList<>();
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    cloudGoals.add(mapToGoal(doc.getData(), doc.getId()));
                }

                // Update Local Manager and UI
                GoalManager.getInstance().setGoals(cloudGoals);
                requireActivity().runOnUiThread(() -> {
                    adapter.setLoading(false);
                    allGoals = cloudGoals;
                    filterGoals(getBinding().tabLayout.getSelectedTabPosition());
                    updateOverallProgress(allGoals);
                });
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.setLoading(false);
                        showError(error);
                    });
                }
            }
        });
    }

    private Goal mapToGoal(Map<String, Object> data, String id) {
        String name = String.valueOf(data.get("name"));
        double target = 0;
        Object t = data.get("targetAmount");
        if (t instanceof Number) target = ((Number) t).doubleValue();
        
        double current = 0;
        Object c = data.get("currentAmount");
        if (c instanceof Number) current = ((Number) c).doubleValue();

        return new Goal(id, name, current, target, "Target Date", R.drawable.test,
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_1));
    }

    private void setupHeader() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.goal_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_add_goal);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddGoalActivity.class);
            startActivity(intent);
        });
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupTabs() {
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterGoals(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void filterGoals(int position) {
        List<Goal> filteredList = new ArrayList<>();
        if (position == 0) { // Active
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

    private void setupRecyclerView() {
        adapter = new GoalAdapter();
        adapter.setOnGoalClickListener(goal -> navigateTo(GoalDetailFragment.newInstance(goal)));
        getBinding().rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvGoals.setAdapter(adapter);
    }

    private void updateOverallProgress(List<Goal> goals) {
        double totalCurrent = 0;
        double totalTarget = 0;

        for (Goal goal : goals) {
            totalCurrent += goal.getCurrentAmount();
            totalTarget += goal.getTargetAmount();
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        getBinding().tvTotalAmount.setText(currencyFormat.format(totalCurrent));

        if (totalTarget > 0) {
            int progress = (int) ((totalCurrent / totalTarget) * 100);
            getBinding().tvOverallPercentage.setText(getString(R.string.percentage_format, progress));
        } else {
            getBinding().tvOverallPercentage.setText(getString(R.string.zero_percent));
        }
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().rvGoals,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_height)
        );
    }
}
