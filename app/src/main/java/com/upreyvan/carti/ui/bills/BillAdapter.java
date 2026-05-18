package com.upreyvan.carti.ui.bills;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemBillBinding;
import com.upreyvan.carti.model.Bill;

public class BillAdapter extends BaseAdapter<Bill, ItemBillBinding> {

    public BillAdapter() {
        super(Bill.DIFF_CALLBACK,
                (inflater, parent) -> ItemBillBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    // Item binding logic is handled in onBindViewHolder for consistent styling
                });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ItemBillBinding> holder, int position) {
        Bill item = getItem(position);
        ItemBillBinding binding = holder.binding;

        binding.tvBillName.setText(item.getName());
        binding.tvBillDate.setText(item.getDate());
        binding.tvBillAmount.setText(item.getAmount());
        binding.tvBillStatus.setText(item.getStatus());
        binding.ivBillIcon.setImageResource(item.getIconResId());

        // Status-based styling following the project's design pattern
        String paidLabel = binding.getRoot().getContext().getString(R.string.status_paid_label);
        if (paidLabel.equalsIgnoreCase(item.getStatus())) {
            binding.tvBillStatus.setBackgroundResource(R.drawable.bg_status_paid);
            binding.tvBillStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_green));
        } else {
            binding.tvBillStatus.setBackgroundResource(R.drawable.bg_status_unpaid);
            binding.tvBillStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_red));
        }

        super.onBindViewHolder(holder, position);
    }
}
