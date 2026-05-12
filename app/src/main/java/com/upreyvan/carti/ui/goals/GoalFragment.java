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
import com.upreyvan.carti.databinding.FragmentGoalBinding;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.util.Utils;

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
        populateMockData();
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

    private void populateMockData() {
        allGoals.clear();

        allGoals.add(new Goal(getString(R.string.mock_goal_bicycle), 3000, 5000, getString(R.string.mock_date_june_30), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_1)));
        allGoals.add(new Goal(getString(R.string.mock_goal_emergency), 2200, 10000, getString(R.string.mock_date_dec_31), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_2)));
        allGoals.add(new Goal(getString(R.string.mock_goal_phone), 8500, 15000, getString(R.string.mock_date_aug_15), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_3)));
        allGoals.add(new Goal(getString(R.string.mock_goal_gift), 1500, 3000, getString(R.string.mock_date_may_12), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_4)));
        allGoals.add(new Goal(getString(R.string.mock_goal_travel), 5000, 12000, getString(R.string.mock_date_oct_20), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_5)));
        allGoals.add(new Goal(getString(R.string.mock_goal_laptop), 12000, 25000, getString(R.string.mock_date_nov_15), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_6)));
        allGoals.add(new Goal(getString(R.string.mock_goal_concert), 2500, 6000, getString(R.string.mock_date_july_05), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_7)));
        allGoals.add(new Goal(getString(R.string.mock_goal_gym), 1000, 2500, getString(R.string.mock_date_june_01), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_8)));
        allGoals.add(new Goal(getString(R.string.mock_goal_investment), 4000, 5000, getString(R.string.mock_date_sept_30), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_9)));
        allGoals.add(new Goal(getString(R.string.mock_goal_shoes), 1800, 3500, getString(R.string.mock_date_aug_22), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_10)));

        filterGoals(getBinding().tabLayout.getSelectedTabPosition());
        updateOverallProgress(allGoals);
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
