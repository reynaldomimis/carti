package com.upreyvan.carti.ui.family;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentMembersBinding;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ItemMemberHorizontalBinding;
import com.upreyvan.carti.databinding.ItemMemberContributionBinding;
import com.upreyvan.carti.util.AvatarHelper;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MembersFragment extends BaseFragment<FragmentMembersBinding> {

    private GenericAdapter<Member, ItemMemberHorizontalBinding> horizontalAdapter;
    private GenericAdapter<Member, ItemMemberContributionBinding> contributionAdapter;
    private MembersViewModel viewModel;
    private String currentUserId;

    @Override
    protected FragmentMembersBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMembersBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(MembersViewModel.class);
        
        setupToolbar(getBinding().toolbar, R.string.family_members_title);
        setupRecyclerViews();
        observeViewModel();
        
        viewModel.refreshData();
    }

    private void observeViewModel() {
        viewModel.getCurrentUserId().observe(getViewLifecycleOwner(), id -> {
            currentUserId = id;
            refreshAdapters();
        });

        viewModel.getMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                getBinding().tvMemberCount.setText(getString(R.string.menu_family_sub_format, members.size()));
                
                List<Member> horizontalList = new ArrayList<>(members);
                horizontalList.add(new Member("invite", "", getString(R.string.label_invite), "", "", android.R.drawable.ic_menu_add, 0));
                
                horizontalAdapter.submitList(horizontalList);
                contributionAdapter.submitList(members);
                
                stopShimmers();
                getBinding().rvMembersHorizontal.setVisibility(View.VISIBLE);
                getBinding().rvContributions.setVisibility(View.VISIBLE);
                getBinding().cardContributions.setVisibility(View.VISIBLE);
                getBinding().toolbar.btnAction.setVisibility(View.GONE);
            }
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> updateBudgetCard());
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> updateBudgetCard());
    }

    private void refreshAdapters() {
        if (horizontalAdapter != null && horizontalAdapter.getCurrentList() != null) {
            horizontalAdapter.submitList(new ArrayList<>(horizontalAdapter.getCurrentList()));
        }
        if (contributionAdapter != null && contributionAdapter.getCurrentList() != null) {
            contributionAdapter.submitList(new ArrayList<>(contributionAdapter.getCurrentList()));
        }
    }

    private void updateBudgetCard() {
        Double income = viewModel.getTotalIncome().getValue();
        Double expense = viewModel.getTotalExpense().getValue();
        if (income == null || expense == null) return;
        
        getBinding().tvBudgetAmount.setText(Utils.formatCurrency(income));
        
        int percent = 0;
        if (income > 0) {
            percent = (int) ((expense / income) * 100);
        }
        
        getBinding().progressBudget.setProgress(Math.min(percent, 100));
        getBinding().tvUsedPercentage.setText(getString(R.string.label_used_percentage, percent));
        getBinding().tvProgressSubtitle.setText(getString(R.string.label_used, percent));
    }

    private void stopShimmers() {
        if (getBinding() == null) return;
        getBinding().shimmerMembersHorizontal.stopShimmer();
        getBinding().shimmerMembersHorizontal.setVisibility(View.GONE);
        getBinding().shimmerContributions.stopShimmer();
        getBinding().shimmerContributions.setVisibility(View.GONE);
    }

    private void setupRecyclerViews() {
        horizontalAdapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberHorizontalBinding.inflate(inflater, parent, false),
                (binding, member, position, count) -> {
                    boolean isMe = member.getId().equals(currentUserId);
                    boolean isInvite = member.getId().equals("invite");

                    if (isInvite) {
                        binding.tvName.setText(member.getTitle());
                        binding.tvRole.setVisibility(View.GONE);
                        binding.ivAvatar.setImageResource(member.getAvatarRes());
                        binding.cvAvatar.setStrokeColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.gray));

                        View.OnClickListener inviteClick = v -> startActivity(new Intent(requireContext(), InviteFamilyActivity.class));
                        binding.cvAvatar.setOnClickListener(inviteClick);
                        binding.ivAvatar.setOnClickListener(inviteClick);
                        binding.tvName.setOnClickListener(inviteClick);
                        binding.getRoot().setOnClickListener(inviteClick);
                    } else {
                        String displayName = isMe ? getString(R.string.label_you) : member.getTitle();
                        binding.tvName.setText(displayName);
                        binding.tvRole.setVisibility(View.VISIBLE);
                        binding.tvRole.setText(member.getDescription());
                        
                        AvatarHelper.loadUserAvatar(requireContext(), binding.ivAvatar, member.getTitle(), member.getAvatarUrl());
                        binding.cvAvatar.setStrokeColor(androidx.core.content.ContextCompat.getColor(requireContext(), isMe ? R.color.carti_primary_green : R.color.border_light));
                    }
                }
        );

        horizontalAdapter.setOnItemClickListener(item -> {
            if ("invite".equals(item.getId())) {
                startActivity(new Intent(requireContext(), InviteFamilyActivity.class));
            }
        });

        getBinding().rvMembersHorizontal.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvMembersHorizontal.setAdapter(horizontalAdapter);

        contributionAdapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberContributionBinding.inflate(inflater, parent, false),
                (binding, member, position, count) -> {
                    boolean isMe = member.getId().equals(currentUserId);
                    String displayName = isMe ? getString(R.string.label_you) : member.getTitle();
                    binding.tvName.setText(displayName);
                    binding.tvContributionLabel.setText(getString(R.string.label_profile_contribution_format, Utils.formatCurrency(member.getAmount())));
                    binding.tvTotalExpense.setText(Utils.formatCurrency(member.getTotalExpense()));
                    
                    AvatarHelper.loadUserAvatar(requireContext(), binding.ivAvatar, member.getTitle(), member.getAvatarUrl());
                }
        );

        contributionAdapter.setOnItemClickListener(item -> navigateTo(com.upreyvan.carti.ui.track.AllTransactionsFragment.newInstance("EXPENSE", item.getId(), item.getTitle())));

        getBinding().rvContributions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvContributions.setAdapter(contributionAdapter);
    }
}
