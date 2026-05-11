package com.upreyvan.carti.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentAddGoalBinding;

public class AddGoalFragment extends BaseFragment<FragmentAddGoalBinding> {

    @Override
    protected FragmentAddGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAddGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupListeners();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_goal_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupListeners() {
        getBinding().btnSaveGoal.setOnClickListener(v -> {
            String name = getBinding().etGoalName.getText().toString();
            String amount = getBinding().etTargetAmount.getText().toString();

            if (name.isEmpty() || amount.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(requireContext(), "Goal Saved Successfully!", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().onBackPressed();
        });

        getBinding().btnPickDate.setOnClickListener(v -> {
            // Show Date Picker
        });
    }

    private void setupDynamicPadding() {
        com.upreyvan.carti.util.Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnSaveGoal,
                0.3f,
                getResources().getDimensionPixelSize(R.dimen.bottom_nav_height)
        );
    }
}