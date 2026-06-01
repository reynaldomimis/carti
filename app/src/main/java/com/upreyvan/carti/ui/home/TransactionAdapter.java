package com.upreyvan.carti.ui.home;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.model.TransactionWithUser;
import com.upreyvan.carti.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends BaseAdapter<TransactionWithUser, ItemTransactionBinding> {

    private boolean isLoading = false;
    private OnTransactionInteractionListener interactionListener;

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

        String username = itemWithUser.getUsername() != null && !itemWithUser.getUsername().isEmpty() 
                ? itemWithUser.getUsername() : "Someone";
        
        String type = item.getType() != null ? item.getType().toUpperCase() : "EXPENSE";
        int amountColor;
        int amountFormatRes;
        String actionLabel;

        switch (type) {
            case "INCOME" -> {
                actionLabel = "Received income";
                amountColor = R.color.green_primary;
                amountFormatRes = R.string.format_income;
            }
            case "DEBT" -> {
                actionLabel = "Recorded a debt";
                amountColor = R.color.status_red;
                amountFormatRes = R.string.format_expense;
            }
            case "GOAL", "GOAL_FUNDS" -> {
                actionLabel = type.equals("GOAL") ? "Started a goal" : "Added funds to goal";
                amountColor = R.color.carti_primary_green;
                amountFormatRes = R.string.format_income;
            }
            case "BILL" -> {
                actionLabel = "Settled a bill";
                amountColor = R.color.status_red;
                amountFormatRes = R.string.format_expense;
            }
            default -> {
                actionLabel = "Added an expense";
                amountColor = R.color.status_red;
                amountFormatRes = R.string.format_expense;
            }
        }

        binding.tvUserAction.setText(username);
        binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));
        binding.tvTitle.setText(item.getCategory()); 

        String note = item.getNote();
        if (note != null && !note.isEmpty()) {
            String filteredNote = note.replace("Logged by Carti AI", "")
                                     .replace("Logged by AI", "")
                                     .trim();
            
            if (filteredNote.isEmpty()) {
                binding.tvDescription.setText(actionLabel);
            } else {
                binding.tvDescription.setText(String.format("%s: %s", actionLabel, filteredNote));
            }
        } else {
            binding.tvDescription.setText(actionLabel);
        }
        
        String formattedAmount = Utils.formatCurrency(item.getAmount());
        binding.tvAmount.setText(binding.getRoot().getContext().getString(amountFormatRes, formattedAmount));
        binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), amountColor));

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
            
            if (likesCount > 0) {
                binding.tvReactionEmoji.setVisibility(View.VISIBLE);
                binding.tvLikesCount.setVisibility(View.VISIBLE);
                binding.tvLikesCount.setText(rNames != null && !rNames.isEmpty() ? rNames : String.valueOf(likesCount));
            } else {
                binding.tvReactionEmoji.setVisibility(View.GONE);
                binding.tvLikesCount.setVisibility(View.GONE);
            }

            if (commentCount > 0) {
                binding.tvCommentsCountSummary.setText(String.format(Locale.getDefault(), "%d comments", commentCount));
                binding.tvCommentsCountSummary.setVisibility(View.VISIBLE);
            } else {
                binding.tvCommentsCountSummary.setVisibility(View.GONE);
            }
        } else {
            binding.layoutReactionsSummary.setVisibility(View.GONE);
        }

        binding.btnLike.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onLikeClick(itemWithUser);
        });
        binding.tvLikesCount.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onViewLikesClick(item, itemWithUser.getReactorNames());
        });
        binding.btnLike.setOnLongClickListener(v -> {
            if (interactionListener != null) {
                showReactionPopup(v, itemWithUser);
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

    private void showReactionPopup(View anchor, TransactionWithUser itemWithUser) {
        View popupView = LayoutInflater.from(anchor.getContext()).inflate(R.layout.layout_reaction_selector, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        
        popupWindow.setElevation(20);
        
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], location[1] - 150);

        View.OnClickListener listener = v -> {
            if (interactionListener != null && v instanceof TextView) {
                interactionListener.onReactionClick(itemWithUser, ((TextView) v).getText().toString());
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

    public void setOnTransactionInteractionListener(OnTransactionInteractionListener listener) {
        this.interactionListener = listener;
    }

    public interface OnTransactionInteractionListener {
        void onLikeClick(TransactionWithUser item);
        void onReactionClick(TransactionWithUser item, String emoji);
        void onCommentClick(Transaction transaction);
        void onViewLikesClick(Transaction transaction, String reactorNames);
    }
}
