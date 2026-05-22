package com.upreyvan.carti.ui.family;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.FragmentMembersBinding;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ItemMemberHorizontalBinding;
import com.upreyvan.carti.databinding.ItemMemberContributionBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.data.remote.AppwriteManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.appwrite.models.User;
import java.util.Map;

public class MembersFragment extends BaseFragment<FragmentMembersBinding> {

    private GenericAdapter<Member, ItemMemberHorizontalBinding> horizontalAdapter;
    private GenericAdapter<Member, ItemMemberContributionBinding> contributionAdapter;
    private MemberRepository repository;
    private PreferenceManager pref;
    private String currentUserId;

    @Override
    protected FragmentMembersBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMembersBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MemberRepository(requireContext());
        pref = PreferenceManager.getInstance(requireContext());
        
        setupToolbar(getBinding().toolbar, R.string.family_members_title);
        setupRecyclerViews();
        setupBudgetCard();
        
        fetchCurrentUser();
        observeMembers();
        
        repository.syncMembersIfNeeded();
    }

    private void fetchCurrentUser() {
        AppwriteManager.getInstance(requireContext()).getCurrentUser(new AppwriteManager.AppwriteCallback<User<Map<String, Object>>>() {
            @Override
            public void onSuccess(User<Map<String, Object>> result) {
                currentUserId = result.getId();
                if (horizontalAdapter != null) {
                    horizontalAdapter.submitList(new ArrayList<>(horizontalAdapter.getCurrentList()));
                }
                if (contributionAdapter != null) {
                    contributionAdapter.submitList(new ArrayList<>(contributionAdapter.getCurrentList()));
                }
            }

            @Override
            public void onError(Throwable error) {
                currentUserId = pref.getUserId();
            }
        });
    }

    private void setupBudgetCard() {
        double income = pref.getTotalIncome();
        double expense = pref.getTotalExpense();
        
        getBinding().tvBudgetAmount.setText(Utils.formatCurrency(income));
        
        int progress = 0;
        if (income > 0) {
            progress = (int) ((expense / income) * 100);
        }
        
        getBinding().progressBudget.setProgress(Math.min(progress, 100));
        getBinding().tvUsedPercentage.setText(getString(R.string.label_used_percentage, progress));
        getBinding().tvProgressSubtitle.setText(getString(R.string.label_used, progress));
    }

    private void observeMembers() {
        repository.getMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                getBinding().tvMemberCount.setText(getString(R.string.menu_family_sub_format, members.size()));
                List<Member> horizontalList = new ArrayList<>(members);
                horizontalList.add(new Member("invite", "", getString(R.string.label_invite), "", "", android.R.drawable.ic_menu_add, 0));
                horizontalAdapter.submitList(horizontalList);
                contributionAdapter.submitList(members);
                getBinding().toolbar.btnAction.setVisibility(View.GONE);
            }
        });
    }

    private void setupRecyclerViews() {
        horizontalAdapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberHorizontalBinding.inflate(inflater, parent, false),
                (binding, member) -> {
                    boolean isMe = member.getId().equals(currentUserId);
                    boolean isInvite = member.getId().equals("invite");

                    if (isInvite) {
                        binding.tvName.setText(member.getTitle());
                        binding.tvRole.setVisibility(View.GONE);
                        binding.ivAvatar.setImageResource(member.getAvatarRes());
                        binding.cvAvatar.setStrokeColor(getResources().getColor(R.color.gray, null));

                        View.OnClickListener inviteClick = v -> {
                            startActivity(new Intent(requireContext(), InviteFamilyActivity.class));
                        };
                        binding.cvAvatar.setOnClickListener(inviteClick);
                        binding.ivAvatar.setOnClickListener(inviteClick);
                        binding.tvName.setOnClickListener(inviteClick);
                        binding.getRoot().setOnClickListener(inviteClick);
                    } else {
                        binding.tvName.setText(isMe ? getString(R.string.placeholder_juan_you) : member.getTitle());
                        binding.tvRole.setVisibility(View.VISIBLE);
                        binding.tvRole.setText(member.getDescription());
                        
                        if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
                            com.bumptech.glide.Glide.with(requireContext())
                                    .load(member.getAvatarUrl())
                                    .placeholder(R.drawable.ai_holder)
                                    .error(R.drawable.ai_holder)
                                    .into(binding.ivAvatar);
                        } else {
                            binding.ivAvatar.setImageResource(member.getAvatarRes() != 0 ? member.getAvatarRes() : R.drawable.ai_holder);
                        }
                        binding.cvAvatar.setStrokeColor(getResources().getColor(isMe ? R.color.carti_primary_green : R.color.border_light, null));
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
                (binding, member) -> {
                    boolean isMe = member.getId().equals(currentUserId);
                    binding.tvName.setText(isMe ? getString(R.string.placeholder_juan_you) : member.getTitle());
                    binding.tvContributionLabel.setText(String.format("Profile Contribution: %s", Utils.formatCurrency(member.getAmount())));
                    
                    if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
                        com.bumptech.glide.Glide.with(requireContext())
                                .load(member.getAvatarUrl())
                                .placeholder(R.drawable.ai_holder)
                                .error(R.drawable.ai_holder)
                                .into(binding.ivAvatar);
                    } else {
                        binding.ivAvatar.setImageResource(member.getAvatarRes() != 0 ? member.getAvatarRes() : R.drawable.ai_holder);
                    }
                    binding.tvBtnViewExpenses.setOnClickListener(v -> navigateTo(new com.upreyvan.carti.ui.track.BreakdownExpenseFragment()));
                }
        );

        contributionAdapter.setOnItemClickListener(item -> {
            navigateTo(new com.upreyvan.carti.ui.track.BreakdownExpenseFragment());
        });

        getBinding().rvContributions.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvContributions.setAdapter(contributionAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) {
            repository.onDestroy();
        }
    }
}
