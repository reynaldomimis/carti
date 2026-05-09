package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.adapters.GoalAdapter;
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

    @Override
    protected FragmentGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupRecyclerView();
        populateMockData();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.goal_title);

        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_add_goal);
        getBinding().layoutToolbar.btnAction.setIconResource(android.R.drawable.ic_input_add);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
            // Handle add goal
        });
    }

    private void setupRecyclerView() {
        adapter = new GoalAdapter();
        getBinding().rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvGoals.setAdapter(adapter);
    }

    private void populateMockData() {
        List<Goal> goals = new ArrayList<>();

        goals.add(new Goal(getString(R.string.mock_goal_bicycle), 3000, 5000, getString(R.string.mock_date_june_30), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_1)));
        goals.add(new Goal(getString(R.string.mock_goal_emergency), 2200, 10000, getString(R.string.mock_date_dec_31), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_2)));
        goals.add(new Goal(getString(R.string.mock_goal_phone), 8500, 15000, getString(R.string.mock_date_aug_15), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_3)));
        goals.add(new Goal(getString(R.string.mock_goal_gift), 1500, 3000, getString(R.string.mock_date_may_12), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_4)));
        goals.add(new Goal(getString(R.string.mock_goal_travel), 5000, 12000, getString(R.string.mock_date_oct_20), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_5)));
        goals.add(new Goal(getString(R.string.mock_goal_laptop), 12000, 25000, getString(R.string.mock_date_nov_15), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_6)));
        goals.add(new Goal(getString(R.string.mock_goal_concert), 2500, 6000, getString(R.string.mock_date_july_05), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_7)));
        goals.add(new Goal(getString(R.string.mock_goal_gym), 1000, 2500, getString(R.string.mock_date_june_01), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_8)));
        goals.add(new Goal(getString(R.string.mock_goal_investment), 4000, 5000, getString(R.string.mock_date_sept_30), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_9)));
        goals.add(new Goal(getString(R.string.mock_goal_shoes), 1800, 3500, getString(R.string.mock_date_aug_22), R.drawable.test, androidx.core.content.ContextCompat.getColor(requireContext(), R.color.goal_card_10)));

        adapter.submitList(goals);
        updateOverallProgress(goals);
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
