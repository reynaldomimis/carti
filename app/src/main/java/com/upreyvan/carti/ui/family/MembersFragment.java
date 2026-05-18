package com.upreyvan.carti.ui.family;

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
import com.upreyvan.carti.databinding.ItemMemberBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MembersFragment extends BaseFragment<FragmentMembersBinding> {

    @Override
    protected FragmentMembersBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMembersBinding.inflate(inflater, container, false);
    }

    private GenericAdapter<Member, ItemMemberBinding> adapter;
    private boolean isLoading = true;
    private MemberRepository repository;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MemberRepository(requireContext());
        
        setupToolbar(getBinding().toolbar, R.string.family_members_title);
        setupRecyclerView();
        
        observeMembers();
        
        repository.syncMembersIfNeeded();
    }

    private void observeMembers() {
        repository.getMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                isLoading = members.isEmpty();
                if (isLoading) {
                    List<Member> placeholders = new ArrayList<>();
                    for (int i = 0; i < 3; i++) placeholders.add(new Member("", "", "", "", "", 0, 0));
                    adapter.submitList(placeholders);
                } else {
                    adapter.submitList(members);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) {
            repository.onDestroy();
        }
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberBinding.inflate(inflater, parent, false),
                (binding, member) -> {
                    View shimmer = binding.getRoot().findViewById(R.id.shimmerView);
                    if (isLoading) {
                        if (shimmer != null) shimmer.setVisibility(View.VISIBLE);
                        binding.layoutContent.setVisibility(View.INVISIBLE);
                    } else {
                        if (shimmer != null) shimmer.setVisibility(View.GONE);
                        binding.layoutContent.setVisibility(View.VISIBLE);
                        binding.tvTitle.setText(member.getTitle());
                        binding.chipRole.setText(member.getDescription());
                        binding.ivAvatar.setImageResource(member.getAvatarRes());
                        String statusText = member.getStatus() + " • " + Utils.formatCurrency(member.getAmount());
                        binding.tvDescription.setText(statusText);
                    }
                }
        );
        getBinding().rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMembers.setAdapter(adapter);
    }
}
