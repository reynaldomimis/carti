package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.BottomSheetGoalActionsBinding;
import com.upreyvan.carti.datasource.AppwriteManager;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.utils.UiHelper;
import java.util.Map;

public class GoalActionsBottomSheetFragment extends BaseBottomSheetFragment<BottomSheetGoalActionsBinding> {

    private Transaction goal;
    private TransactionRepository repository;

    public static GoalActionsBottomSheetFragment newInstance(Transaction goal) {
        GoalActionsBottomSheetFragment fragment = new GoalActionsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("goal_json", new Gson().toJson(goal));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String json = getArguments().getString("goal_json");
            goal = new Gson().fromJson(json, Transaction.class);
        }
        repository = TransactionRepository.getInstance(requireContext());
    }

    @Override
    protected BottomSheetGoalActionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetGoalActionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());

        getBinding().btnContribute.setOnClickListener(v -> {
            GoalContributeBottomSheetFragment.newInstance(goal).show(getParentFragmentManager(), "CONTRIBUTE_GOAL");
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
