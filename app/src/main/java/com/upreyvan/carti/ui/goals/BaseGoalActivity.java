package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.GoalRepository;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.ActivityAddGoalBinding;
import com.upreyvan.carti.databinding.ItemMemberAvatarSelectBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class BaseGoalActivity extends BaseActivity<ActivityAddGoalBinding> {

    protected GoalRepository goalRepository;
    protected MemberRepository memberRepository;
    protected GenericAdapter<Member, ItemMemberAvatarSelectBinding> memberAdapter;
    protected final Set<String> selectedMemberIds = new HashSet<>();

    @Override
    protected ActivityAddGoalBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddGoalBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        goalRepository = new GoalRepository(this);
        memberRepository = new MemberRepository(this);
        
        setupDynamicPadding();
        setupToolbar();
        setupDatePicker();
        setupMemberSelection();
        setupBaseListeners();
        
        initForm();
    }

    protected abstract void initForm();

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnCreateGoal,
                1f,
                20
        );
    }

    protected void setupToolbar() {
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
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

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void setupMemberSelection() {
        memberAdapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberAvatarSelectBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.ivAvatar.setImageResource(item.getAvatarRes());
                    binding.tvName.setText(item.getTitle());

                    binding.vOverlay.setVisibility(View.GONE);
                    binding.ivSelected.setVisibility(View.VISIBLE);
                    binding.cvAvatar.setStrokeColor(getColor(R.color.carti_primary_green));
                    binding.cvAvatar.setStrokeWidth(Utils.dpToPx(this, 2));

                    binding.getRoot().setOnClickListener(v -> {
                        List<Member> current = new ArrayList<>(memberAdapter.getCurrentList());
                        current.remove(item);
                        selectedMemberIds.remove(item.getId());
                        memberAdapter.submitList(current);
                    });
                }
        );

        getBinding().rvFamilyContribution.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvFamilyContribution.setAdapter(memberAdapter);
    }

    private void setupBaseListeners() {
        getBinding().btnAddMember.setOnClickListener(v -> {
            MemberPickerBottomSheet bottomSheet = MemberPickerBottomSheet.newInstance(selectedMemberIds);
            bottomSheet.setListener(members -> {
                selectedMemberIds.clear();
                for (Member m : members) {
                    selectedMemberIds.add(m.getId());
                }
                memberAdapter.submitList(new ArrayList<>(members));
            });
            bottomSheet.show(getSupportFragmentManager(), "MEMBER_PICKER");
        });

        getBinding().btnCreateGoal.setOnClickListener(v -> onSaveClicked());
    }

    protected abstract void onSaveClicked();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (goalRepository != null) goalRepository.onDestroy();
        if (memberRepository != null) memberRepository.onDestroy();
    }
}
