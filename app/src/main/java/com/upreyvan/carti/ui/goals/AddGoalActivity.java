package com.upreyvan.carti.ui.goals;

import android.content.Intent;
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
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.ActivityAddGoalBinding;
import com.upreyvan.carti.databinding.ItemMemberAvatarSelectBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import com.upreyvan.carti.util.ToastHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AddGoalActivity extends BaseActivity<ActivityAddGoalBinding> {

    private GoalRepository goalRepository;
    private MemberRepository memberRepository;
    private GenericAdapter<Member, ItemMemberAvatarSelectBinding> memberAdapter;
    private final Set<String> selectedMemberIds = new HashSet<>();

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
        setupListeners();
        
        loadMembers();
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnCreateGoal,
                1f,
                20
        );
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_goal_title);
        getBinding().layoutToolbar.btnBack.setImageResource(R.drawable.ic_close);
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
                        // Option to remove if needed
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

    private void loadMembers() {
        // We don't load all members by default anymore. 
        // User starts with empty list.
        memberAdapter.submitList(new ArrayList<>());
    }

    private void setupListeners() {
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

        getBinding().btnCreateGoal.setOnClickListener(v -> {
            if (!checkNetwork()) return;

            if (Validator.isEmpty(getBinding().etGoalName) || Validator.isEmpty(getBinding().etTargetAmount)) {
                showToast(getString(R.string.msg_fill_all_fields), ToastHelper.Status.WARNING);
                return;
            }

            String name = getBinding().etGoalName.getText().toString().trim();
            double targetAmount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
            String date = getBinding().etTargetDate.getText().toString().trim();
            
            showLoading(true, getString(R.string.msg_saving_goal));

            new ApiHelper(this).addGoal(name, targetAmount, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    String id = String.valueOf(result.get("$id"));
                    String familyId = new com.upreyvan.carti.data.local.PreferenceManager(AddGoalActivity.this).getFamilyId();
                    
                    goalRepository.saveLocally(
                            new com.upreyvan.carti.model.Goal(id, familyId, name, "", 0, targetAmount, date, R.drawable.ic_image,
                                    androidx.core.content.ContextCompat.getColor(AddGoalActivity.this, R.color.goal_card_1))
                    );

                    showLoading(false);
                    showToast(getString(R.string.msg_goal_saved_success), ToastHelper.Status.SUCCESS);
                    finish();
                }

                @Override
                public void onError(Throwable error) {
                    showLoading(false);
                    showToast(getString(R.string.err_generic, error.getMessage()), ToastHelper.Status.ERROR);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (goalRepository != null) goalRepository.onDestroy();
        if (memberRepository != null) memberRepository.onDestroy();
    }
}
