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

    
        String username = itemWithUser.getUsername() != null ? itemWithUser.getUsername() : "Someone";
        String actionText;
        if ("EXPENSE".equalsIgnoreCase(item.getType())) {
            actionText = binding.getRoot().getContext().getString(R.string.action_added_expense, username);
        } else {
            actionText = binding.getRoot().getContext().getString(R.string.action_added_income, username);
        }
        binding.tvUserAction.setText(actionText);
        binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));

        // Body: Transaction Details
        binding.tvTitle.setText(item.getTitle());
        binding.tvDescription.setText(item.getNote());
        
        String formattedAmount = Utils.formatCurrency(item.getAmount());
        if ("EXPENSE".equalsIgnoreCase(item.getType())) {
            binding.tvAmount.setText(binding.getRoot().getContext().getString(R.string.format_expense, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_red));
        } else {
            binding.tvAmount.setText(binding.getRoot().getContext().getString(R.string.format_income, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.green_primary));
        }

        // Avatar Binding
        if (itemWithUser.getUserAvatarUrl() != null && !itemWithUser.getUserAvatarUrl().isEmpty()) {
            Glide.with(binding.getRoot().getContext())
                    .load(itemWithUser.getUserAvatarUrl())
                    .placeholder(R.drawable.ai_holder)
                    .error(R.drawable.ai_holder)
                    .into(binding.ivAvatar);
        } else if (itemWithUser.getUserAvatarRes() != 0) {
            binding.ivAvatar.setImageResource(itemWithUser.getUserAvatarRes());
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ai_holder);
        }

        // Footer: Likes and Comments
        binding.tvLikesCount.setText(String.valueOf(item.getLikesCount()));
        int commentCount = item.getCommentCount();
        binding.tvCommentsCount.setText(commentCount > 0 ? String.valueOf(commentCount) : "Comment");

        // Click Listeners (to be implemented in Fragment/Activity)
        binding.layoutLikes.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onLikeClick(item);
        });
        binding.layoutLikes.setOnLongClickListener(v -> {
            if (interactionListener != null) {
                showReactionPopup(v, item);
                return true;
            }
            return false;
        });
        binding.btnComment.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onCommentClick(item);
        });
        binding.btnMore.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onMoreClick(item, v);
        });
    }

    private void showReactionPopup(View anchor, Transaction transaction) {
        View popupView = android.view.LayoutInflater.from(anchor.getContext()).inflate(R.layout.layout_reaction_selector, null);
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        
        popupWindow.setElevation(20);
        
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, 
                location[0], location[1] - 150);

        View.OnClickListener listener = v -> {
            if (interactionListener != null && v instanceof android.widget.TextView) {
                interactionListener.onReactionClick(transaction, ((android.widget.TextView) v).getText().toString());
            }
            popupWindow.dismiss();
        };

        popupView.findViewById(R.id.reac_like).setOnClickListener(listener);
        popupView.findViewById(R.id.reac_love).setOnClickListener(listener);
        popupView.findViewById(R.id.reac_haha).setOnClickListener(listener);
        popupView.findViewById(R.id.reac_wow).setOnClickListener(listener);
        popupView.findViewById(R.id.reac_sad).setOnClickListener(listener);
        popupView.findViewById(R.id.reac_angry).setOnClickListener(listener);
    }

    private OnTransactionInteractionListener interactionListener;
    public void setOnTransactionInteractionListener(OnTransactionInteractionListener listener) {
        this.interactionListener = listener;
    }

    public interface OnTransactionInteractionListener {
        void onLikeClick(Transaction transaction);
        void onReactionClick(Transaction transaction, String emoji);
        void onCommentClick(Transaction transaction);
        void onMoreClick(Transaction transaction, View view);
    }
}
