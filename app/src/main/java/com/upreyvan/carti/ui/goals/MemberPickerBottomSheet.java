package com.upreyvan.carti.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.repository.MemberRepository;
import com.upreyvan.carti.databinding.ItemMemberAvatarSelectBinding;
import com.upreyvan.carti.databinding.LayoutBottomSheetMemberPickerBinding;
import com.upreyvan.carti.model.Member;
import com.upreyvan.carti.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemberPickerBottomSheet extends BaseBottomSheetFragment<LayoutBottomSheetMemberPickerBinding> {

    private MemberRepository memberRepository;
    private GenericAdapter<Member, ItemMemberAvatarSelectBinding> adapter;
    private OnMembersSelectedListener listener;
    private final Set<String> selectedIds = new HashSet<>();
    private boolean isMultiSelect = true;

    public interface OnMembersSelectedListener {
        void onMembersSelected(List<Member> members);
    }

    public static MemberPickerBottomSheet newInstance(Set<String> selectedIds) {
        return newInstance(selectedIds, true);
    }

    public static MemberPickerBottomSheet newInstance(Set<String> selectedIds, boolean isMultiSelect) {
        MemberPickerBottomSheet fragment = new MemberPickerBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList("selected_ids", new ArrayList<>(selectedIds));
        args.putBoolean("is_multi_select", isMultiSelect);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnMembersSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected LayoutBottomSheetMemberPickerBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutBottomSheetMemberPickerBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            isMultiSelect = getArguments().getBoolean("is_multi_select", true);
            List<String> selected = getArguments().getStringArrayList("selected_ids");
            if (selected != null) {
                selectedIds.addAll(selected);
            }
        }

        getBinding().btnSelectAll.setVisibility(isMultiSelect ? View.VISIBLE : View.GONE);
        
        memberRepository = new MemberRepository(requireContext());

        setupRecyclerView();
        setupListeners();
        loadMembers();
    }

    private void setupListeners() {
        getBinding().btnSelectAll.setOnClickListener(v -> {
            if (!isMultiSelect) return;

            List<Member> members = adapter.getCurrentList();
            if (members.isEmpty()) return;

            boolean allSelected = true;
            for (Member m : members) {
                if (!selectedIds.contains(m.getId())) {
                    allSelected = false;
                    break;
                }
            }

            if (allSelected) {
                selectedIds.clear();
            } else {
                for (Member m : members) {
                    selectedIds.add(m.getId());
                }
            }
            adapter.notifyDataSetChanged();
            updateSelectAllText();
        });

        getBinding().btnConfirmSelection.setOnClickListener(v -> {
            if (listener != null) {
                List<Member> selectedMembers = new ArrayList<>();
                for (Member m : adapter.getCurrentList()) {
                    if (selectedIds.contains(m.getId())) {
                        selectedMembers.add(m);
                    }
                }
                listener.onMembersSelected(selectedMembers);
            }
            dismiss();
        });
    }

    private void updateSelectAllText() {
        if (!isMultiSelect) return;

        List<Member> members = adapter.getCurrentList();
        if (members.isEmpty()) {
            getBinding().btnSelectAll.setText("Select All");
            return;
        }

        boolean allSelected = true;
        for (Member m : members) {
            if (!selectedIds.contains(m.getId())) {
                allSelected = false;
                break;
            }
        }
        getBinding().btnSelectAll.setText(allSelected ? "Deselect All" : "Select All");
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter<>(
                Member.DIFF_CALLBACK,
                (inflater, parent) -> ItemMemberAvatarSelectBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.ivAvatar.setImageResource(item.getAvatarRes());
                    binding.tvName.setText(item.getTitle());
                    
                    boolean isSelected = selectedIds.contains(item.getId());

                    // Visual indicators for selection
                    binding.vOverlay.setVisibility(isSelected ? View.GONE : View.VISIBLE);
                    binding.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                    binding.cvAvatar.setStrokeColor(isSelected ? 
                            requireContext().getColor(R.color.carti_primary_green) : 
                            requireContext().getColor(R.color.border_subtle));
                    binding.cvAvatar.setStrokeWidth(isSelected ? Utils.dpToPx(requireContext(), 2) : Utils.dpToPx(requireContext(), 1));
                }
        );

        adapter.setOnItemClickListener(item -> {
            if (isMultiSelect) {
                if (selectedIds.contains(item.getId())) {
                    selectedIds.remove(item.getId());
                } else {
                    selectedIds.add(item.getId());
                }
            } else {
                String id = item.getId();
                boolean wasSelected = selectedIds.contains(id);
                selectedIds.clear();
                if (!wasSelected) {
                    selectedIds.add(id);
                }
            }
            adapter.notifyDataSetChanged();
            updateSelectAllText();
        });

        getBinding().rvMembers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvMembers.setAdapter(adapter);
    }

    private void loadMembers() {
        memberRepository.getMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                adapter.submitList(members);
                updateSelectAllText();
            }
        });
        memberRepository.syncMembersIfNeeded();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (memberRepository != null) memberRepository.onDestroy();
    }
}
