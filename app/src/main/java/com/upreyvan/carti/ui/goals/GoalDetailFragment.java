package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.FragmentGoalDetailBinding;
import com.upreyvan.carti.databinding.ItemGoalHistoryBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.ui.common.AddFundsFragment;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.ValueHelper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GoalDetailFragment extends BaseFragment<FragmentGoalDetailBinding> {

    private String goalId;
    private Transaction goal;
    private GoalDetailViewModel viewModel;
    private GenericAdapter<HistoryItem, ItemGoalHistoryBinding> historyAdapter;

    public static GoalDetailFragment newInstance(String goalId) {
        GoalDetailFragment fragment = new GoalDetailFragment();
        Bundle args = new Bundle();
        args.putString(com.upreyvan.carti.util.Constants.Keys.KEY_TRANSACTION_ID, goalId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            goalId = getArguments().getString(com.upreyvan.carti.util.Constants.Keys.KEY_TRANSACTION_ID);
        }
    }

    @Override
    protected FragmentGoalDetailBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentGoalDetailBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GoalDetailViewModel.class);
        setupDynamicPadding();
        setupHistory();
        observeViewModel();
    }

    private void setupHistory() {
        getBinding().rvProgressHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyAdapter = new GenericAdapter<>(
                new DiffUtil.ItemCallback<HistoryItem>() {
                    @Override public boolean areItemsTheSame(@NonNull HistoryItem oldItem, @NonNull HistoryItem newItem) { return oldItem.equals(newItem); }
                    @Override public boolean areContentsTheSame(@NonNull HistoryItem oldItem, @NonNull HistoryItem newItem) { return oldItem.equals(newItem); }
                },
                (inflater, parent) -> ItemGoalHistoryBinding.inflate(inflater, parent, false),
                (binding, item, pos, count) -> {
                    binding.tvHistoryDate.setText(item.date);
                    binding.tvHistoryAmount.setText(item.amount);
                }
        );
        getBinding().rvProgressHistory.setAdapter(historyAdapter);
    }

    private void observeViewModel() {
        viewModel.getGoal(goalId).observe(getViewLifecycleOwner(), g -> {
            if (g != null) {
                this.goal = g;
                updateUI();
            }
        });

        viewModel.getHistory(goalId).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                List<HistoryItem> items = new ArrayList<>();
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
                currencyFormat.setMaximumFractionDigits(0);

                for (TransactionWithUser tWithU : transactions) {
                    Transaction t = tWithU.getTransaction();
                    items.add(new HistoryItem(
                            Utils.formatDate(t.getTimestampMillis()),
                            currencyFormat.format(t.getAmount())
                    ));
                }
                historyAdapter.submitList(items);
            }
        });
    }

    private void updateUI() {
        if (goal == null) return;
        setupToolbar();
        setupGoalData();
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
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnAddFunds,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_height)
        );
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(ValueHelper.toStr(goal.getTitle()));
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_edit);
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
            UpdateGoalBottomSheetFragment bottomSheet = UpdateGoalBottomSheetFragment.newInstance(goal.getId());
            bottomSheet.show(getChildFragmentManager(), "UPDATE_GOAL");
        });
    }

    private void setupGoalData() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(0);

        getBinding().tvGoalName.setText(ValueHelper.toStr(goal.getTitle()));
        getBinding().tvGoalProgressAmount.setText(String.format(Locale.getDefault(), "%s / %s",
                currencyFormat.format(goal.getAmount()),
                currencyFormat.format(goal.getTargetAmount())));

        int progress = goal.getProgress();
        getBinding().progressGoal.setProgress(progress);
        getBinding().tvStatusComplete.setText(getString(R.string.goal_progress_complete_format, progress));

        getBinding().tvTargetDate.setText(ValueHelper.toStr(goal.getTargetDate()));
        getBinding().tvMonthlyTarget.setText(currencyFormat.format(1000));

        if (goal.getIconRes() != 0) {
            getBinding().ivGoalImage.setImageResource(goal.getIconRes());
            getBinding().ivGoalImage.setColorFilter(null);
            getBinding().ivGoalImage.setPadding(0, 0, 0, 0);
        }
    }

    private static class HistoryItem {
        final String date;
        final String amount;

        HistoryItem(String date, String amount) {
            this.date = date;
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HistoryItem that = (HistoryItem) o;
            return date.equals(that.date) && amount.equals(that.amount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, amount);
        }
    }
}
