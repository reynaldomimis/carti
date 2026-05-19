package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.GoalRepository;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.ItemMemberAvatarSelectBinding;
import com.upreyvan.carti.databinding.LayoutBottomSheetUpdateGoalBinding;
import com.upreyvan.carti.model.Goal;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UpdateGoalBottomSheetFragment extends BaseBottomSheetFragment<LayoutBottomSheetUpdateGoalBinding> {

    public static final String ARG_GOAL_ID = "arg_goal_id";
    private String goalId;
    private Goal currentGoal;
    private GoalRepository goalRepository;
    private MemberRepository memberRepository;
    private GenericAdapter<Member, ItemMemberAvatarSelectBinding> memberAdapter;
    private final Set<String> selectedMemberIds = new HashSet<>();

    public static UpdateGoalBottomSheetFragment newInstance(String goalId) {
        UpdateGoalBottomSheetFragment fragment = new UpdateGoalBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GOAL_ID, goalId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            goalId = getArguments().getString(ARG_GOAL_ID);
        }
        goalRepository = new GoalRepository(requireContext());
        memberRepository = new MemberRepository(requireContext());
    }

    @Override
    protected LayoutBottomSheetUpdateGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutBottomSheetUpdateGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDatePicker();
        setupMemberSelection();
        setupListeners();
        observeGoal();
    }

    private void observeGoal() {
        goalRepository.getGoalById(goalId).observe(getViewLifecycleOwner(), goal -> {
            if (goal != null) {
                currentGoal = goal;
                preFillData();
            }
        });
    }

    private void preFillData() {
        getBinding().etGoalName.setText(currentGoal.getTitle());
        getBinding().etTargetAmount.setText(String.valueOf(currentGoal.getTargetAmount()));
        getBinding().etTargetDate.setText(currentGoal.getTargetDate());

        String contributorIdsStr = currentGoal.getContributorIds();
        if (contributorIdsStr != null && !contributorIdsStr.isEmpty()) {
            selectedMemberIds.clear();
            selectedMemberIds.addAll(Arrays.asList(contributorIdsStr.split(",")));
        }
        
        loadMembers();
    }

    private void loadMembers() {
        memberRepository.getMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                memberAdapter.submitList(members);
            }
        });
    }

    private void setupDatePicker() {
        getBinding().etTargetDate.setOnClickListener(v -> showDatePicker());
        getBinding().tilTargetDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_target_date)
                .setTheme(R.style.CartiDatePicker)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            getBinding().etTargetDate.setText(sdf.format(calendar.getTime()));
        });

        datePicker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void setupMemberSelection() {
        memberAdapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberAvatarSelectBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.ivAvatar.setImageResource(item.getAvatarRes());
                    binding.tvName.setText(item.getTitle());

                    boolean isSelected = selectedMemberIds.contains(item.getId());
                    binding.vOverlay.setVisibility(isSelected ? View.GONE : View.VISIBLE);
                    binding.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                    binding.cvAvatar.setStrokeColor(requireContext().getColor(isSelected ? R.color.carti_primary_green : R.color.border_subtle));
                    binding.cvAvatar.setStrokeWidth(Utils.dpToPx(requireContext(), 2));

                    binding.getRoot().setOnClickListener(v -> {
                        if (selectedMemberIds.contains(item.getId())) {
                            selectedMemberIds.remove(item.getId());
                        } else {
                            selectedMemberIds.add(item.getId());
                        }
                        memberAdapter.notifyItemChanged(memberAdapter.getCurrentList().indexOf(item));
                    });
                }
        );

        getBinding().rvFamilyMembers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvFamilyMembers.setAdapter(memberAdapter);
    }

    private void setupListeners() {
        getBinding().btnUpdateGoal.setOnClickListener(v -> onUpdateClicked());
    }

    private void onUpdateClicked() {
        if (!checkNetwork()) return;
        if (currentGoal == null) return;

        if (getBinding().etGoalName.getText() == null || Validator.isEmpty(getBinding().etGoalName) ||
                getBinding().etTargetAmount.getText() == null || Validator.isEmpty(getBinding().etTargetAmount)) {
            showToast(R.string.msg_fill_all_fields, ToastHelper.Status.WARNING);
            return;
        }

        String name = getBinding().etGoalName.getText().toString().trim();
        double targetAmount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String date = getBinding().etTargetDate.getText() != null ? getBinding().etTargetDate.getText().toString().trim() : "";
        String contributorIds = String.join(",", selectedMemberIds);

        showLoading(true, "Updating goal...");

        currentGoal.setTitle(name);
        currentGoal.setTargetAmount(targetAmount);
        currentGoal.setTargetDate(date);
        currentGoal.setContributorIds(contributorIds);

        goalRepository.updateGoal(currentGoal, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (isAdded()) {
                    showLoading(false);
                    showToast("Goal updated successfully", ToastHelper.Status.SUCCESS);
                    dismiss();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    showLoading(false);
                    showToast(getString(R.string.err_generic, error.getMessage()), ToastHelper.Status.ERROR);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (goalRepository != null) goalRepository.onDestroy();
        if (memberRepository != null) memberRepository.onDestroy();
    }
}
