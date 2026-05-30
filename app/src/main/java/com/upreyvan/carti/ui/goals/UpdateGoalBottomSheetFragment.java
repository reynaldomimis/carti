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
import com.upreyvan.carti.data.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.List;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.ItemMemberAvatarSelectBinding;
import com.upreyvan.carti.databinding.LayoutBottomSheetUpdateGoalBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ValueHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UpdateGoalBottomSheetFragment extends BaseBottomSheetFragment<LayoutBottomSheetUpdateGoalBinding> {
    public static final String ARG_GOAL_ID = "arg_goal_id";
    private String goalId; private Transaction currentGoal; private TransactionRepository transactionRepository; private MemberRepository memberRepository;
    private GenericAdapter<Member, ItemMemberAvatarSelectBinding> memberAdapter; private final Set<String> selectedMemberIds = new HashSet<>();

    public static UpdateGoalBottomSheetFragment newInstance(String id) {
        UpdateGoalBottomSheetFragment f = new UpdateGoalBottomSheetFragment(); Bundle a = new Bundle(); a.putString(ARG_GOAL_ID, id); f.setArguments(a); return f;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); if (getArguments() != null) goalId = getArguments().getString(ARG_GOAL_ID);
        transactionRepository = TransactionRepository.getInstance(requireContext()); memberRepository = new MemberRepository(requireContext());
    }

    @Override protected LayoutBottomSheetUpdateGoalBinding inflateBinding(@NonNull LayoutInflater i, @Nullable ViewGroup c) { return LayoutBottomSheetUpdateGoalBinding.inflate(i, c, false); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) { super.onViewCreated(view, savedInstanceState); setupDatePicker(); setupMemberSelection(); setupListeners(); observeGoal(); }

    private void observeGoal() { transactionRepository.getTransactionById(goalId).observe(getViewLifecycleOwner(), g -> { if (g != null) { currentGoal = g.getTransaction(); preFillData(); } }); }

    private void preFillData() {
        getBinding().etGoalName.setText(ValueHelper.toStr(currentGoal.getTitle())); getBinding().etTargetAmount.setText(String.valueOf(currentGoal.getTargetAmount())); getBinding().etTargetDate.setText(ValueHelper.toStr(currentGoal.getDueDate()));
        selectedMemberIds.clear(); List<String> members = currentGoal.getMembers(); if (members != null) selectedMemberIds.addAll(members); loadMembers();
    }

    private void loadMembers() { memberRepository.getMembers().observe(getViewLifecycleOwner(), members -> { if (members != null) memberAdapter.submitList(members); }); }

    private void setupDatePicker() { View.OnClickListener show = v -> showDatePicker(); getBinding().etTargetDate.setOnClickListener(show); getBinding().tilTargetDate.setEndIconOnClickListener(show); }

    private void showDatePicker() {
        MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.label_target_date).setTheme(R.style.CartiDatePicker).setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        dp.addOnPositiveButtonClickListener(s -> { Calendar c = Calendar.getInstance(); c.setTimeInMillis(s); getBinding().etTargetDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(c.getTime())); });
        dp.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void setupMemberSelection() {
        memberAdapter = new GenericAdapter<>(Member.DIFF_CALLBACK, (i, p) -> ItemMemberAvatarSelectBinding.inflate(i, p, false), (b, item) -> {
            b.ivAvatar.setImageResource(item.getAvatarRes()); b.tvName.setText(item.getTitle()); boolean sel = selectedMemberIds.contains(item.getId());
            b.vOverlay.setVisibility(sel ? View.GONE : View.VISIBLE); b.ivSelected.setVisibility(sel ? View.VISIBLE : View.GONE);
            b.cvAvatar.setStrokeColor(requireContext().getColor(sel ? R.color.carti_primary_green : R.color.border_subtle)); b.cvAvatar.setStrokeWidth(Utils.dpToPx(requireContext(), 2));
            b.getRoot().setOnClickListener(v -> { if (selectedMemberIds.contains(item.getId())) selectedMemberIds.remove(item.getId()); else selectedMemberIds.add(item.getId()); memberAdapter.notifyItemChanged(memberAdapter.getCurrentList().indexOf(item)); });
        });
        getBinding().rvFamilyMembers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)); getBinding().rvFamilyMembers.setAdapter(memberAdapter);
    }

    private void setupListeners() { getBinding().btnUpdateGoal.setOnClickListener(v -> onUpdateClicked()); }

    private void onUpdateClicked() {
        if (!checkNetwork() || currentGoal == null) return;
        if (Validator.isEmpty(getBinding().etGoalName) || Validator.isEmpty(getBinding().etTargetAmount)) { showToast(R.string.msg_fill_all_fields, ToastHelper.Status.WARNING); return; }
        showLoading(true, "Updating goal..."); currentGoal.setTitle(getBinding().etGoalName.getText().toString().trim()); currentGoal.setTargetAmount(Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim()));
        currentGoal.setDueDate(getBinding().etTargetDate.getText().toString().trim()); currentGoal.setMembers(new ArrayList<>(selectedMemberIds));
        transactionRepository.updateTransaction(currentGoal, new com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> r) { if (isAdded()) { showLoading(false); showToast("Goal updated successfully", ToastHelper.Status.SUCCESS); dismiss(); } }
            @Override public void onError(Throwable e) { if (isAdded()) { showLoading(false); showToast(getString(R.string.err_generic, e.getMessage()), ToastHelper.Status.ERROR); } }
        });
    }
}
