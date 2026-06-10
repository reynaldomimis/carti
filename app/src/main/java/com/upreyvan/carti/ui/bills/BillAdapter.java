package com.upreyvan.carti.ui.bills;

import androidx.core.content.ContextCompat;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemBillBinding;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.utils.Utils;

public class BillAdapter extends BaseAdapter<Bill, ItemBillBinding> {

    public BillAdapter() {
        super(Bill.DIFF_CALLBACK,
                (inflater, parent) -> ItemBillBinding.inflate(inflater, parent, false),
                (b, item, pos, count) -> {
                    b.tvBillName.setText(item.getName());
                    b.tvBillCategory.setText(item.getCategory());
                    
                    String dateText = item.getDate();
                    if (dateText != null && !dateText.isEmpty()) {
                        b.tvDueBadge.setText(b.getRoot().getContext().getString(R.string.add_bill_at_date, dateText).replace("Add Bill for ", "Due "));
                        b.tvDueBadge.setVisibility(android.view.View.VISIBLE);
                    } else {
                        b.tvDueBadge.setVisibility(android.view.View.GONE);
                    }

                    b.tvBillAmount.setText(Utils.formatCurrency(item.getAmount()));
                    
                    if (item.getIconResId() != 0) {
                        b.ivBillIcon.setImageResource(item.getIconResId());
                    } else {
                        b.ivBillIcon.setImageResource(R.drawable.ic_calendar);
                    }

                    String paidLabel = b.getRoot().getContext().getString(R.string.status_paid_label);
                    if (paidLabel.equalsIgnoreCase(item.getStatus())) {
                        b.tvDueBadge.setBackgroundResource(R.drawable.bg_status_paid);
                        b.tvDueBadge.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.green_primary));
                    } else {
                        b.tvDueBadge.setBackgroundResource(R.drawable.bg_status_unpaid);
                        b.tvDueBadge.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.status_red));
                    }
                });
    }
}
