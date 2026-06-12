package com.upreyvan.carti.ui.bills;

import androidx.core.content.ContextCompat;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemBillBinding;
import com.upreyvan.carti.managers.CategoryManager;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.Category;
import com.upreyvan.carti.utils.Utils;

public class BillAdapter extends BaseAdapter<Bill, ItemBillBinding> {

    public BillAdapter() {
        super(Bill.DIFF_CALLBACK,
                (inflater, parent) -> ItemBillBinding.inflate(inflater, parent, false),
                (b, item, pos, count) -> {
                    b.tvBillName.setText(item.getName());
                    
                    String dateText = item.getDate();
                    if (dateText != null && !dateText.isEmpty()) {
                        b.tvDueBadge.setText(b.getRoot().getContext().getString(R.string.add_bill_at_date, dateText).replace("Add Bill for ", "Due "));
                        b.tvDueBadge.setVisibility(android.view.View.VISIBLE);
                    } else {
                        b.tvDueBadge.setVisibility(android.view.View.GONE);
                    }

                    b.tvBillAmount.setText(Utils.formatCurrency(item.getAmount()));
                    
                    // Centralized Style Implementation for Bills
                    Category c = CategoryManager.getInstance(b.getRoot().getContext()).getCategoryByName(item.getCategory());
                    if (c != null) {
                        b.ivBillIcon.setImageResource(c.getIconRes());
                        b.cardIcon.setCardBackgroundColor(ContextCompat.getColor(b.getRoot().getContext(), c.getBackgroundColor()));
                        b.ivBillIcon.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(b.getRoot().getContext(), c.getIconColor())));
                    } else {
                        b.ivBillIcon.setImageResource(item.getIconResId() != 0 ? item.getIconResId() : R.drawable.ic_calendar);
                        b.cardIcon.setCardBackgroundColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.surface_variant));
                        b.ivBillIcon.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(b.getRoot().getContext(), R.color.carti_primary_green)));
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
