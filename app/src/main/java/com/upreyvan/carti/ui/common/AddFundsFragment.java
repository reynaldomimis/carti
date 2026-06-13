package com.upreyvan.carti.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAddFundsBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.utils.Utils;
import com.upreyvan.carti.utils.ValueHelper;
import java.text.NumberFormat;
import java.util.Locale;

public class AddFundsFragment extends BaseFragment<FragmentAddFundsBinding> {

    private Transaction goal;

    public static AddFundsFragment newInstance(Transaction goal) {
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
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void setupGoalHeader() {
        if (goal == null) return;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(0);

        getBinding().tvGoalName.setText(ValueHelper.toStr(goal.getTitle()));
        getBinding().tvCurrentProgress.setText(getString(R.string.goal_progress_amount_format,
                currencyFormat.format(goal.getAmount()),
                currencyFormat.format(goal.getTargetAmount())));

        int progress = goal.getProgress();
        getBinding().progressGoal.setProgress(progress);
        getBinding().tvProgressPercent.setText(getString(R.string.percentage_format, progress));
    }

    private void setupListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            String amountStr = getBinding().etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                showToast("Please enter an amount", com.upreyvan.carti.utils.UiHelper.Status.WARNING);
                return;
            }
            showToast("Saved successfully!", com.upreyvan.carti.utils.UiHelper.Status.SUCCESS);
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnSave,
                0.3f,
                0
        );
    }
}