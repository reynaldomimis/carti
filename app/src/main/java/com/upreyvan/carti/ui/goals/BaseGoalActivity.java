package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.ActivityAddGoalBinding;
import com.upreyvan.carti.databinding.ItemMemberAvatarSelectBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseGoalActivity extends BaseActivity<ActivityAddGoalBinding> {
    protected TransactionRepository transactionRepository;
    protected MemberRepository memberRepository;
    protected GenericAdapter<Member, ItemMemberAvatarSelectBinding> memberAdapter;
    protected final Set<String> selectedMemberIds = new HashSet<>();

    @Override protected ActivityAddGoalBinding inflateBinding(LayoutInflater inflater) { return ActivityAddGoalBinding.inflate(inflater); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(this);
        memberRepository = MemberRepository.getInstance(this);
        setupDynamicPadding(); setupToolbar(); setupDatePicker(); setupMemberSelection(); setupBaseListeners();
        initForm();
    }

    protected abstract void initForm();

    private void setupDynamicPadding() { 
        setupDynamicPadding(getBinding().layoutToolbar.getRoot(), getBinding().btnCreateGoal); 
    }

    protected void setupToolbar() { getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish()); }

    private void setupDatePicker() {
        View.OnClickListener show = v -> {
            MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.label_target_date).setTheme(R.style.CartiDatePicker).setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
            dp.addOnPositiveButtonClickListener(s -> { Calendar c = Calendar.getInstance(); c.setTimeInMillis(s); getBinding().etTargetDate.setText(Utils.formatDateShort(c)); });
            dp.show(getSupportFragmentManager(), "DATE_PICKER");
        };
        getBinding().etTargetDate.setOnClickListener(show); getBinding().tilTargetDate.setEndIconOnClickListener(show);
    }

    private void setupMemberSelection() {
        memberAdapter = new GenericAdapter<>(Member.DIFF_CALLBACK, (inf, p) -> ItemMemberAvatarSelectBinding.inflate(inf, p, false), (b, item) -> {
            b.ivAvatar.setImageResource(item.getAvatarRes()); b.tvName.setText(item.getTitle()); b.vOverlay.setVisibility(View.GONE); b.ivSelected.setVisibility(View.VISIBLE);
            b.cvAvatar.setStrokeColor(getColor(R.color.carti_primary_green)); b.cvAvatar.setStrokeWidth(Utils.dpToPx(this, 2));
            b.getRoot().setOnClickListener(v -> { List<Member> cur = new ArrayList<>(memberAdapter.getCurrentList()); cur.remove(item); selectedMemberIds.remove(item.getId()); memberAdapter.submitList(cur); });
        });
        getBinding().rvFamilyContribution.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)); getBinding().rvFamilyContribution.setAdapter(memberAdapter);
    }

    protected void setupBaseListeners() {
        getBinding().btnAddMember.setOnClickListener(v -> {
            MemberPickerBottomSheet sheet = MemberPickerBottomSheet.newInstance(selectedMemberIds);
            sheet.setListener(members -> {
                selectedMemberIds.clear();
                for (Member m : members) selectedMemberIds.add(m.getId());
                memberAdapter.submitList(new ArrayList<>(members));
            });
            sheet.show(getSupportFragmentManager(), "MEMBER_PICKER");
        });
        getBinding().btnCreateGoal.setOnClickListener(v -> onSaveClicked());
    }

    protected void preSelectAllMembers() {
        memberRepository.getMembers().observe(this, members -> { if (members != null && !members.isEmpty() && selectedMemberIds.isEmpty()) { selectedMemberIds.clear(); for (Member m : members) selectedMemberIds.add(m.getId()); memberAdapter.submitList(new ArrayList<>(members)); } });
    }

    protected abstract void onSaveClicked();
}
