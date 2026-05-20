package com.upreyvan.carti.ui.home;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends BaseAdapter<TransactionWithUser, ItemTransactionBinding> {

    private boolean isLoading = false;

    public TransactionAdapter() {
        super(TransactionWithUser.DIFF_CALLBACK,
                (inflater, parent) -> ItemTransactionBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                });
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            List<TransactionWithUser> placeholders = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                TransactionWithUser placeholder = new TransactionWithUser();
                placeholder.setTransaction(new Transaction());
                placeholders.add(placeholder);
            }
            submitList(placeholders);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ItemTransactionBinding> holder, int position) {
        TransactionWithUser itemWithUser = getItem(position);
        Transaction item = itemWithUser.getTransaction();
        ItemTransactionBinding binding = holder.binding;

        if (isLoading) {
            binding.shimmerView.getRoot().setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.INVISIBLE);
            return;
        }

        binding.shimmerView.getRoot().setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);

        binding.tvTitle.setText(itemWithUser.getUserName() != null ? itemWithUser.getUserName() : item.getName());
        binding.tvDescription.setText(item.getNote());
        binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));

        binding.divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
        
        String formattedAmount = Utils.formatCurrency(item.getAmount());
        if ("EXPENSE".equalsIgnoreCase(item.getType())) {
            binding.tvAmount.setText(binding.getRoot().getContext().getString(R.string.format_expense, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_red));
        } else {
            binding.tvAmount.setText(binding.getRoot().getContext().getString(R.string.format_income, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.green_primary));
        }

        binding.ivIcon.setColorFilter(null);
        if (itemWithUser.getUserAvatarUrl() != null && !itemWithUser.getUserAvatarUrl().isEmpty()) {
            Glide.with(binding.getRoot().getContext())
                    .load(itemWithUser.getUserAvatarUrl())
                    .placeholder(R.drawable.ai_holder)
                    .error(R.drawable.ai_holder)
                    .into(binding.ivIcon);
            binding.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), android.R.color.transparent));
        } else if (itemWithUser.getUserAvatarRes() != 0) {
            binding.ivIcon.setImageResource(itemWithUser.getUserAvatarRes());
            binding.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), android.R.color.transparent));
        } else {
            binding.ivIcon.setImageResource(item.getIconRes());
            if (item.getIconColor() != 0) {
                int iconColor = item.getIconColor();
                binding.ivIcon.setColorFilter(iconColor);
                int bgColor = ColorUtils.setAlphaComponent(iconColor, 25);
                binding.cvIconBg.setCardBackgroundColor(bgColor);
            } else {
                binding.cvIconBg.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.surface_variant));
            }
        }
    }
}
