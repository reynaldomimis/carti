package com.upreyvan.carti.ui.bills;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemBillBinding;
import com.upreyvan.carti.managers.CategoryManager;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.Category;
import com.upreyvan.carti.utils.Utils;

public class BillAdapter extends BaseAdapter<Bill, ItemBillBinding> {

    public interface OnBillInteractionListener {
        void onDelete(Bill bill);
        void onEdit(Bill bill, View anchor);
    }

    private OnBillInteractionListener interactionListener;

    public void setOnBillInteractionListener(OnBillInteractionListener listener) {
        this.interactionListener = listener;
    }

    public BillAdapter() {
        super(Bill.DIFF_CALLBACK,
                (inflater, parent) -> ItemBillBinding.inflate(inflater, parent, false),
                (b, item, pos, count) -> {
                    b.tvBillName.setText(item.getName());
                    b.tvBillAmount.setText(Utils.formatCurrency(item.getAmount()));
                    
                    String dateText = item.getDate();
                    String status = item.getStatus();

                    if ("Overdue".equalsIgnoreCase(status)) {
                        b.tvDueBadge.setText(R.string.label_overdue);
                        b.tvDueBadge.setVisibility(android.view.View.VISIBLE);
                        b.tvDueBadge.setBackgroundResource(R.drawable.bg_status_unpaid);
                        b.tvDueBadge.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.status_red));
                    } else if ("Due Today".equalsIgnoreCase(status)) {
                        b.tvDueBadge.setText(R.string.label_due_today);
                        b.tvDueBadge.setVisibility(android.view.View.VISIBLE);
                        b.tvDueBadge.setBackgroundResource(R.drawable.bg_status_unpaid);
                        b.tvDueBadge.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.status_red));
                    } else if ("Due Tomorrow".equalsIgnoreCase(status)) {
                        b.tvDueBadge.setText(R.string.label_due_tomorrow);
                        b.tvDueBadge.setVisibility(android.view.View.VISIBLE);
                        b.tvDueBadge.setBackgroundResource(R.drawable.bg_status_unpaid);
                        b.tvDueBadge.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.status_red));
                    } else if (dateText != null && !dateText.isEmpty()) {
                        b.tvDueBadge.setText(b.getRoot().getContext().getString(R.string.add_bill_at_date, dateText).replace("Add Bill for ", "Due "));
                        b.tvDueBadge.setVisibility(android.view.View.VISIBLE);
                        
                        String paidLabel = b.getRoot().getContext().getString(R.string.status_paid_label);
                        if (paidLabel.equalsIgnoreCase(status)) {
                            b.tvDueBadge.setBackgroundResource(R.drawable.bg_status_paid);
                            b.tvDueBadge.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.green_primary));
                        } else {
                            b.tvDueBadge.setBackgroundResource(R.drawable.bg_status_unpaid);
                            b.tvDueBadge.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.text_secondary));
                        }
                    } else {
                        b.tvDueBadge.setVisibility(android.view.View.GONE);
                    }
                });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ItemBillBinding> holder, int position) {
        super.onBindViewHolder(holder, position);
        Bill item = getItem(position);
        holder.binding.btnOptions.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onEdit(item, v);
        });
        
        holder.binding.getRoot().setOnLongClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onDelete(item);
                return true;
            }
            return false;
        });
    }
}
