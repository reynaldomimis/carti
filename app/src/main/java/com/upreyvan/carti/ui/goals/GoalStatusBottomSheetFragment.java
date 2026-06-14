package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.BottomSheetGoalStatusBinding;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.utils.UiHelper;
import java.util.Map;

public class GoalStatusBottomSheetFragment extends BaseBottomSheetFragment<BottomSheetGoalStatusBinding> {

    private Transaction goal;
    private TransactionRepository repository;
    private boolean isCheckStatusMode = false;

    public static GoalStatusBottomSheetFragment newInstance(Transaction goal) {
        return newInstance(goal, com.upreyvan.carti.utils.GoalHelper.isTargetReached(goal));
    }

    public static GoalStatusBottomSheetFragment newInstance(Transaction goal, boolean isCheckStatusMode) {
        GoalStatusBottomSheetFragment fragment = new GoalStatusBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("goal_json", new Gson().toJson(goal));
        args.putBoolean("check_status_mode", isCheckStatusMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String json = getArguments().getString("goal_json");
            goal = new Gson().fromJson(json, Transaction.class);
            isCheckStatusMode = getArguments().getBoolean("check_status_mode", false);
        }
        repository = TransactionRepository.getInstance(requireContext());
    }

    @Override
    protected BottomSheetGoalStatusBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetGoalStatusBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupListeners();
    }

    private void setupUI() {
        if (isCheckStatusMode) {
            getBinding().tvTitle.setText("Manage Goal Status");
            getBinding().btnComplete.setVisibility(View.VISIBLE);
            getBinding().btnExtend.setVisibility(View.VISIBLE);
            getBinding().btnContribute.setVisibility(View.GONE);
        } else {
            getBinding().tvTitle.setText("Goal Actions");
            getBinding().btnComplete.setVisibility(View.GONE);
            getBinding().btnExtend.setVisibility(View.GONE);
            getBinding().btnContribute.setVisibility(View.VISIBLE);
        }
        getBinding().btnCancel.setVisibility(View.VISIBLE);
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        
        getBinding().btnContribute.setOnClickListener(v -> {
            GoalContributeBottomSheetFragment.newInstance(goal).show(getParentFragmentManager(), "CONTRIBUTE_GOAL");
            dismiss();
        });

        getBinding().btnComplete.setOnClickListener(v -> {
            showLoading(true, "Achieving goal...");
            repository.completeGoalCascade(goal, new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    showLoading(false);
                    showToast("Congratulations! Goal achieved.", UiHelper.Status.SUCCESS);
                    dismiss();
                }
                @Override
                public void onError(Throwable error) {
                    showLoading(false);
                    showToast("Completion failed: " + error.getMessage(), UiHelper.Status.ERROR);
                }
            });
        });

        getBinding().btnExtend.setOnClickListener(v -> {
            ExtendGoalBottomSheetFragment.newInstance(goal).show(getParentFragmentManager(), "EXTEND_GOAL");
            dismiss();
        });

        getBinding().btnCancel.setOnClickListener(v -> cancelGoal());
    }

    private void cancelGoal() {
        showLoading(true, "Canceling goal...");
        goal.setStatus("CANCELED");
        repository.updateItem(goal.getType(), goal.getId(), goal, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                showLoading(false);
                showToast("Goal has been canceled.", UiHelper.Status.INFO);
                dismiss();
            }

            @Override
            public void onError(Throwable error) {
                showLoading(false);
                showToast("Action failed: " + error.getMessage(), UiHelper.Status.ERROR);
            }
        });
    }
}
