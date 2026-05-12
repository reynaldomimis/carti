package com.upreyvan.carti.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.databinding.ItemProfileMenuBinding;
import com.upreyvan.carti.model.ProfileMenuItem;

import java.util.List;

public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileMenuAdapter.ViewHolder> {

    private final List<ProfileMenuItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ProfileMenuItem item);
    }

    public ProfileMenuAdapter(List<ProfileMenuItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileMenuBinding binding = ItemProfileMenuBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfileMenuBinding binding;

        ViewHolder(ItemProfileMenuBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProfileMenuItem item, OnItemClickListener listener) {
            binding.ivMenuIcon.setImageResource(item.getIconResId());
            binding.tvMenuTitle.setText(item.getTitleResId());
            binding.tvMenuSubTitle.setText(item.getSubTitle());
            binding.divider.setVisibility(item.isShowDivider() ? View.VISIBLE : View.GONE);
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
