package com.upreyvan.carti.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DiffUtil;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends BaseAdapter<Transaction, ItemTransactionBinding> {

    private boolean isLoading = false;

    public TransactionAdapter() {
        super(new DiffUtil.ItemCallback<Transaction>() {
            @Override
            public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                if (oldItem.getTitle() == null || newItem.getTitle() == null) return false;
                return oldItem.getTitle().equals(newItem.getTitle());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.equals(newItem);
            }
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
    protected ItemTransactionBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemTransactionBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(ItemTransactionBinding binding, Transaction item) {
        if (isLoading) {
            binding.shimmerView.getRoot().setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.INVISIBLE);
            return;
        }

        binding.shimmerView.getRoot().setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);

        binding.tvTitle.setText(item.getTitle());
        binding.tvTimestamp.setText(com.upreyvan.carti.util.Utils.getTimeAgo(item.getTimestampMillis()));
        binding.tvAmount.setText(item.getAmount());
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
