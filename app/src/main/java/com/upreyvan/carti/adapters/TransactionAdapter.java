package com.upreyvan.carti.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.model.Transaction;

public class TransactionAdapter extends BaseAdapter<Transaction, ItemTransactionBinding> {

    public TransactionAdapter() {
        super(new DiffUtil.ItemCallback<Transaction>() {
            @Override
            public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.getTitle().equals(newItem.getTitle());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    @Override
    protected ItemTransactionBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemTransactionBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(ItemTransactionBinding binding, Transaction item) {
        binding.tvTitle.setText(item.getTitle());
        binding.tvTimestamp.setText(item.getTimestamp());
        binding.tvAmount.setText(item.getAmount());
        binding.ivIcon.setImageResource(item.getIconRes());
        binding.cvIconBg.setCardBackgroundColor(item.getIconBgColor());
    }
}
