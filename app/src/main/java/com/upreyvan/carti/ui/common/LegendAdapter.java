package com.upreyvan.carti.ui.common;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.databinding.ItemLegendExpenseBinding;
import com.upreyvan.carti.model.ExpenseCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.LegendViewHolder> {

    private List<ExpenseCategory> items = new ArrayList<>();

    public void submitList(List<ExpenseCategory> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLegendExpenseBinding binding = ItemLegendExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LegendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LegendViewHolder extends RecyclerView.ViewHolder {
        private final ItemLegendExpenseBinding binding;

        public LegendViewHolder(ItemLegendExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ExpenseCategory item) {
            binding.viewColor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(item.getColor()));
            binding.tvCategory.setText(item.getName());
            
            String amountFormatted = String.format(Locale.getDefault(), "₱%,.0f", item.getAmount());
            String text = String.format(Locale.getDefault(), "%s (%.0f%%)", amountFormatted, item.getPercentage());
            binding.tvAmountPercent.setText(text);
        }
    }
}