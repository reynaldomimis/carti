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

        String username = itemWithUser.getUsername();
        if (username == null || username.isEmpty()) {
            username = "Someone";
        }
        
        String type = item.getType() != null ? item.getType().toUpperCase() : "EXPENSE";
        int amountColor;
        int amountFormatRes;
        String actionLabel;

        switch (type) {
            case "INCOME":
                actionLabel = "Received income";
                amountColor = R.color.green_primary;
                amountFormatRes = R.string.format_income;
                break;
            case "DEBT":
                actionLabel = "Recorded a debt";
                amountColor = R.color.status_red;
                amountFormatRes = R.string.format_expense;
                break;
            case "GOAL":
                actionLabel = "Started a goal";
                amountColor = R.color.carti_primary_green;
                amountFormatRes = R.string.format_income;
                break;
            case "GOAL_FUNDS":
                actionLabel = "Added funds to goal";
                amountColor = R.color.carti_primary_green;
                amountFormatRes = R.string.format_income;
                break;
            case "BILL":
                actionLabel = "Settled a bill";
                amountColor = R.color.status_red;
                amountFormatRes = R.string.format_expense;
                break;
            case "EXPENSE":
            default:
                actionLabel = "Added an expense";
                amountColor = R.color.status_red;
                amountFormatRes = R.string.format_expense;
                break;
        }

        binding.tvUserAction.setText(username);
        binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));
        
        binding.tvTitle.setText(item.getCategory()); 

        String note = item.getNote();
        if (note != null && !note.isEmpty()) {
            binding.tvDescription.setText(String.format("%s: %s", actionLabel, note));
        } else {
            binding.tvDescription.setText(actionLabel);
        }
        binding.tvDescription.setVisibility(View.VISIBLE);
        
        String formattedAmount = Utils.formatCurrency(item.getAmount());
        binding.tvAmount.setText(binding.getRoot().getContext().getString(amountFormatRes, formattedAmount));
        binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), amountColor));

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

        String myReaction = itemWithUser.getMyReaction();
        String lastEmoji = item.getLastEmoji();

        if (myReaction != null && !myReaction.isEmpty()) {
            binding.tvBtnLikeIcon.setText(myReaction);
            binding.tvBtnLikeText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.carti_primary_green));
        } else {
            binding.tvBtnLikeIcon.setText("👍");
            binding.tvBtnLikeText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
        }

        binding.tvReactionEmoji.setText(myReaction != null && !myReaction.isEmpty() ? myReaction : (lastEmoji != null && !lastEmoji.isEmpty() ? lastEmoji : "👍"));
        
        int likesCount = item.getLikesCount();
        int commentCount = item.getCommentCount();
        String rNames = itemWithUser.getReactorNames();

        if (likesCount > 0 || commentCount > 0) {
            binding.layoutReactionsSummary.setVisibility(View.VISIBLE);
            
            // Likes summary
            if (likesCount > 0) {
                binding.tvReactionEmoji.setVisibility(View.VISIBLE);
                binding.tvLikesCount.setVisibility(View.VISIBLE);
                if (rNames != null && !rNames.isEmpty()) {
                    binding.tvLikesCount.setText(rNames);
                } else {
                    binding.tvLikesCount.setText(String.valueOf(likesCount));
                }
            } else {
                binding.tvReactionEmoji.setVisibility(View.GONE);
                binding.tvLikesCount.setVisibility(View.GONE);
            }

            // Comments summary
            if (commentCount > 0) {
                binding.tvCommentsCountSummary.setText(String.format("%d comments", commentCount));
                binding.tvCommentsCountSummary.setVisibility(View.VISIBLE);
            } else {
                binding.tvCommentsCountSummary.setVisibility(View.GONE);
            }
        } else {
            binding.layoutReactionsSummary.setVisibility(View.GONE);
        }

        binding.tvCommentsCount.setText("Comment");

        binding.btnLike.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onLikeClick(item);
        });
        binding.tvLikesCount.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onViewLikesClick(item, itemWithUser.getReactorNames());
        });
        binding.btnLike.setOnLongClickListener(v -> {
            if (interactionListener != null) {
                showReactionPopup(v, item);
                return true;
            }
            return false;
        });
        binding.btnComment.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onCommentClick(item);
        });

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(itemWithUser);
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
        void onViewLikesClick(Transaction transaction, String reactorNames);
    }
}
