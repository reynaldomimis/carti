package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentGoalDetailBinding;
import com.upreyvan.carti.databinding.ItemGoalHistoryBinding;
import com.upreyvan.carti.model.Goal;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoalDetailFragment extends BaseFragment<FragmentGoalDetailBinding> {

    private Goal goal;

    public static GoalDetailFragment newInstance(Goal goal) {
        GoalDetailFragment fragment = new GoalDetailFragment();
        fragment.goal = goal;
        return fragment;
    }

    @Override
    protected FragmentGoalDetailBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentGoalDetailBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupGoalData();
        setupHistoryList();
        setupListeners();
    }

    private void setupListeners() {
        getBinding().btnAddFunds.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, AddFundsFragment.newInstance(goal))
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void setupDynamicPadding() {
        com.upreyvan.carti.util.Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnAddFunds,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_height)
        );
    }

    private void setupToolbar() {
        if (goal != null) {
            getBinding().layoutToolbar.tvToolbarTitle.setText(goal.getTitle());
        } else {
            getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.goal_details_title);
        }
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupGoalData() {
        if (goal == null) return;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(0);

        getBinding().tvGoalName.setText(goal.getTitle());
        getBinding().tvGoalProgressAmount.setText(String.format("%s / %s",
                currencyFormat.format(goal.getCurrentAmount()),
                currencyFormat.format(goal.getTargetAmount())));

        int progress = goal.getProgress();
        getBinding().progressGoal.setProgress(progress);
        getBinding().tvStatusComplete.setText(getString(R.string.goal_progress_complete_format, progress));

        getBinding().tvTargetDate.setText(goal.getTargetDate());
        getBinding().tvMonthlyTarget.setText(currencyFormat.format(1000));

        if (goal.getImageRes() != 0) {
            getBinding().ivGoalImage.setImageResource(goal.getImageRes());
            getBinding().ivGoalImage.setColorFilter(null);
            getBinding().ivGoalImage.setPadding(0, 0, 0, 0); // No padding for hero image look
        }
    }

    private void setupHistoryList() {
        List<HistoryItem> history = new ArrayList<>();
        history.add(new HistoryItem("May 22, 2024", "₱500"));
        history.add(new HistoryItem("May 15, 2024", "₱300"));
        history.add(new HistoryItem("May 1, 2024", "₱200"));

        getBinding().rvProgressHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvProgressHistory.setAdapter(new HistoryAdapter(history));
    }

    private static class HistoryItem {
        String date;
        String amount;

        HistoryItem(String date, String amount) {
            this.date = date;
            this.amount = amount;
        }
    }

    private static class HistoryAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<HistoryItem> items;

        HistoryAdapter(List<HistoryItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemGoalHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = items.get(position);
            holder.binding.tvHistoryDate.setText(item.date);
            holder.binding.tvHistoryAmount.setText(item.amount);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final ItemGoalHistoryBinding binding;
            ViewHolder(ItemGoalHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}