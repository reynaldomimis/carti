package com.upreyvan.carti.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAddFundsBinding;
import com.upreyvan.carti.model.Goal;

import java.text.NumberFormat;
import java.util.Locale;

public class AddFundsFragment extends BaseFragment<FragmentAddFundsBinding> {

    private Goal goal;

    public static AddFundsFragment newInstance(Goal goal) {
        AddFundsFragment fragment = new AddFundsFragment();
        fragment.goal = goal;
        return fragment;
    }

    @Override
    protected FragmentAddFundsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAddFundsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupGoalHeader();
        setupListeners();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_to_goal_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupGoalHeader() {
        if (goal == null) return;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(0);

        getBinding().tvGoalName.setText(goal.getTitle());
        getBinding().tvCurrentProgress.setText(getString(R.string.label_current_amount,
                currencyFormat.format(goal.getCurrentAmount()),
                currencyFormat.format(goal.getTargetAmount())));

        int progress = goal.getProgress();
        getBinding().progressGoal.setProgress(progress);
        getBinding().tvProgressPercent.setText(getString(R.string.percentage_format, progress));
    }

    private void setupListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            String amount = getBinding().etAmount.getText().toString();
            if (amount.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(requireContext(), "Saved successfully!", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().onBackPressed();
        });

        getBinding().cardSalary.setOnClickListener(v -> selectSource(0));
        getBinding().cardChallenge.setOnClickListener(v -> selectSource(1));
        getBinding().cardOther.setOnClickListener(v -> selectSource(2));
    }

    private void selectSource(int index) {
        // Simple visual toggle logic could go here
    }

    private void setupDynamicPadding() {
        com.upreyvan.carti.util.Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnSave,
                0.3f,
                0
        );
    }
}