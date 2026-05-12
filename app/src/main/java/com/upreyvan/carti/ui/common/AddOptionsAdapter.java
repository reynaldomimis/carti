package com.upreyvan.carti.ui.common;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.databinding.ItemAddOptionBinding;
import com.upreyvan.carti.model.AddOption;

import java.util.List;

public class AddOptionsAdapter extends RecyclerView.Adapter<AddOptionsAdapter.ViewHolder> {

    private final List<AddOption> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AddOption item);
    }

    public AddOptionsAdapter(List<AddOption> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddOptionBinding binding = ItemAddOptionBinding.inflate(
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAddOptionBinding binding;

        ViewHolder(ItemAddOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AddOption item, OnItemClickListener listener) {
            binding.ivOptionIcon.setImageResource(item.getIconResId());
            binding.ivOptionIcon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), item.getIconTintResId())));
            
            binding.viewBgTint.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), item.getBgTintResId())));
            
            binding.tvOptionTitle.setText(item.getTitleResId());
            binding.tvOptionDesc.setText(item.getDescResId());
            
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
