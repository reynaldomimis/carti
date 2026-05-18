package com.upreyvan.carti.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends BaseAdapter<Transaction, ItemTransactionBinding> {

    private boolean isLoading = false;

    public TransactionAdapter() {
        super(Transaction.DIFF_CALLBACK,
                (inflater, parent) -> ItemTransactionBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    // Logic moved to internal bind for isLoading handling
                });
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            List<Transaction> placeholders = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                placeholders.add(new Transaction());
            }
            submitList(placeholders);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ItemTransactionBinding> holder, int position) {
        Transaction item = getItem(position);
        ItemTransactionBinding binding = holder.binding;

        if (isLoading) {
            binding.shimmerView.getRoot().setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.INVISIBLE);
            return;
        }

        binding.shimmerView.getRoot().setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);

        binding.tvTitle.setText(item.getTitle());
        binding.tvTimestamp.setText(com.upreyvan.carti.util.Utils.getTimeAgo(item.getTimestampMillis()));
        
        String formattedAmount = com.upreyvan.carti.util.Utils.formatCurrency(item.getAmount());
        if ("EXPENSE".equalsIgnoreCase(item.getType())) {
            binding.tvAmount.setText(binding.getRoot().getContext().getString(R.string.format_expense, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_red));
        } else {
            binding.tvAmount.setText(binding.getRoot().getContext().getString(R.string.format_income, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.green_primary));
        }

        binding.ivIcon.setImageResource(item.getIconRes());

        if (item.getIconColor() != 0) {
            int iconColor = item.getIconColor();
            binding.ivIcon.setColorFilter(iconColor);

            int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);
            binding.cvIconBg.setCardBackgroundColor(bgColor);
        } else {
            binding.ivIcon.setColorFilter(null);
            binding.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.surface_variant));
        }
    }
}
