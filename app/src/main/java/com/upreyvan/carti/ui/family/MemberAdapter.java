package com.upreyvan.carti.ui.family;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import com.upreyvan.carti.databinding.ItemMemberBinding;
import com.upreyvan.carti.model.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Member> members = new ArrayList<>();
    private boolean isLoading = false;

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    public void submitList(List<Member> members) {
        this.members = new ArrayList<>(members);
        this.isLoading = false;
        notifyDataSetChanged();
    }

    public void addMember(Member member) {
        if (!members.contains(member)) {
            members.add(member);
            notifyItemInserted(members.size() - 1);
        }
    }

    public void updateMember(Member member) {
        int index = members.indexOf(member);
        if (index != -1) {
            members.set(index, member);
            notifyItemChanged(index);
        }
    }

    public void removeMember(String memberId) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getId().equals(memberId)) {
                members.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemberBinding binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MemberViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        if (isLoading) {
            holder.showShimmer();
        } else {
            holder.bind(members.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return isLoading ? 3 : members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final ItemMemberBinding binding;

        public MemberViewHolder(ItemMemberBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Member member) {
            binding.shimmerView.getRoot().setVisibility(View.GONE);
            binding.layoutContent.setVisibility(View.VISIBLE);
            binding.tvMemberName.setText(member.getName());
            binding.chipRole.setText(member.getRole());
            binding.ivMemberAvatar.setImageResource(member.getAvatarRes());

            String statusText = member.getStatus() + " • ₱" + String.format(Locale.getDefault(), "%.2f", member.getSalary());
            binding.tvMemberStatus.setText(statusText);
        }

        public void showShimmer() {
            binding.shimmerView.getRoot().setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.INVISIBLE);
        }
    }
}