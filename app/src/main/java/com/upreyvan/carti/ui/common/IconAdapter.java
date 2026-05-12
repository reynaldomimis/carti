package com.upreyvan.carti.ui.common;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.databinding.ItemIconChoiceBinding;
import com.upreyvan.carti.model.IconChoice;

import java.util.ArrayList;
import java.util.List;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {

    private List<IconChoice> icons;
    private final OnIconClickListener listener;

    public interface OnIconClickListener {
        void onIconClick(IconChoice icon);
    }

    public IconAdapter(List<IconChoice> icons, OnIconClickListener listener) {
        this.icons = icons;
        this.listener = listener;
    }

    public void updateList(List<IconChoice> newList) {
        this.icons = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemIconChoiceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IconChoice icon = icons.get(position);
        holder.binding.ivIcon.setImageResource(icon.getIconRes());
        holder.binding.getRoot().setOnClickListener(v -> listener.onIconClick(icon));
    }

    @Override
    public int getItemCount() {
        return icons.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemIconChoiceBinding binding;
        ViewHolder(ItemIconChoiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
