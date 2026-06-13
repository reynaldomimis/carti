package com.upreyvan.carti.ui.track;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.ItemTransactionBinding;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.utils.AvatarHelper;
import com.upreyvan.carti.utils.DialogHelper;
import com.upreyvan.carti.utils.Utils;
import java.util.Locale;
import java.util.Objects;

public class TransactionAdapter extends ListAdapter<TransactionWithUser, TransactionAdapter.ViewHolder> {

    private OnTransactionInteractionListener interactionListener;
    private OnItemClickListener itemClickListener;
    private String currentUserId;

    public TransactionAdapter(String currentUserId) {
        super(TransactionWithUser.DIFF_CALLBACK);
        this.currentUserId = currentUserId;
    }

    public interface OnTransactionInteractionListener {
        void onLikeClick(TransactionWithUser item);
        void onReactionClick(TransactionWithUser item, String emoji);
        void onCommentClick(Transaction transaction);
        void onViewLikesClick(Transaction transaction, String reactorNames);
        void onEditClick(Transaction transaction);
        void onDeleteClick(Transaction transaction);
    }

    public interface OnItemClickListener {
        void onItemClick(TransactionWithUser item);
    }

    public void setOnTransactionInteractionListener(OnTransactionInteractionListener listener) {
        this.interactionListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), interactionListener, itemClickListener, currentUserId);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        ViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TransactionWithUser itemWithUser, OnTransactionInteractionListener interactionListener, OnItemClickListener itemClickListener, String currentUserId) {
            Transaction item = itemWithUser.getTransaction();
            
            boolean isOwner = item.getUserId() != null && item.getUserId().equalsIgnoreCase(currentUserId);
            binding.btnOptions.setVisibility(isOwner ? View.VISIBLE : View.GONE);

            if (isOwner) {
                binding.btnOptions.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(binding.getRoot().getContext(), v);
                    popup.getMenuInflater().inflate(R.menu.menu_transaction_options, popup.getMenu());
                    popup.setOnMenuItemClickListener(menuItem -> {
                        int id = menuItem.getItemId();
                        if (id == R.id.action_edit) {
                            if (interactionListener != null) interactionListener.onEditClick(item);
                            return true;
                        } else if (id == R.id.action_delete) {
                            DialogHelper.showConfirmation(
                                    binding.getRoot().getContext(),
                                    "Delete Transaction?",
                                    "Are you sure you want to delete this transaction?",
                                    "Delete",
                                    () -> {
                                        if (interactionListener != null) interactionListener.onDeleteClick(item);
                                    }
                            );
                            return true;
                        }
                        return false;
                    });
                    popup.show();
                });
            }

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
                    actionLabel = Objects.equals(type, "GOAL") ? "Started a goal" : "Added funds to goal";
                    amountColor = R.color.carti_primary_green;
                    amountFormatRes = R.string.format_income;
                }
                case "BILL" -> {
                    actionLabel = "Settled a bill";
                    amountColor = R.color.status_red;
                    amountFormatRes = R.string.format_expense;
                }
                case "ALLOCATION" -> {
                    actionLabel = "Planned budget set";
                    amountColor = R.color.carti_primary_green;
                    amountFormatRes = R.string.format_income;
                }
                default -> {
                    actionLabel = "Added an expense";
                    amountColor = R.color.status_red;
                    amountFormatRes = R.string.format_expense;
                }
            }

            binding.tvUserAction.setText(username);
            
            String status = item.getStatus();
            if (Transaction.STATUS_PENDING.equals(status) || Transaction.STATUS_SYNCING.equals(status)) {
                binding.getRoot().setAlpha(0.6f);
                binding.tvTimestamp.setText("Sending...");
            } else {
                binding.getRoot().setAlpha(1.0f);
                binding.tvTimestamp.setText(Utils.getTimeAgo(item.getTimestampMillis()));
            }

            String category = item.getCategory() != null ? item.getCategory() : "Expense";
            String subCategory = item.getSubCategory();
            if (subCategory != null && !subCategory.isEmpty()) {
                binding.tvTitle.setText(String.format("%s (%s)", category, subCategory));
            } else {
                binding.tvTitle.setText(category);
            }

            String note = item.getNote();
            String filteredNote = "";
            if (note != null && !note.isEmpty()) {
                filteredNote = note.replace("Logged by Carti AI", "").replace("Logged by AI", "").trim();
            }
            binding.tvDescription.setText(!filteredNote.isEmpty() ? filteredNote : actionLabel);
            binding.tvDescription.setVisibility(View.VISIBLE);
            
            String formattedAmount = Utils.formatCurrency(item.getAmount());
            binding.tvAmount.setText(binding.getRoot().getContext().getString(amountFormatRes, formattedAmount));
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), amountColor));

            AvatarHelper.loadUserAvatar(binding.getRoot().getContext(), binding.ivAvatar, username, itemWithUser.getUserAvatarUrl());

            String myReaction = itemWithUser.getMyReaction();
            String lastEmoji = item.getLastEmoji();

            if (myReaction != null && !myReaction.isEmpty()) {
                binding.tvBtnLikeIcon.setText(myReaction);
                binding.tvBtnLikeText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.carti_primary_blue));
            } else {
                binding.tvBtnLikeIcon.setText("👍");
                binding.tvBtnLikeText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.icon_electricity));
            }

            binding.tvReactionEmoji.setText(myReaction != null && !myReaction.isEmpty() ? myReaction : (lastEmoji != null && !lastEmoji.isEmpty() ? lastEmoji : "👍"));
            
            int likesCount = item.getLikesCount();
            int commentCount = item.getCommentCount();
            String rNames = itemWithUser.getReactorNames();


            if (likesCount > 0) {
                binding.layoutLikesSummaryClickable.setVisibility(View.VISIBLE);
                if (likesCount == 1 && rNames != null && !rNames.isEmpty()) {
                    binding.tvLikesCount.setText(rNames);
                } else {
                    binding.tvLikesCount.setText(String.valueOf(likesCount));
                }
            } else {
                binding.layoutLikesSummaryClickable.setVisibility(View.GONE);
            }


            if (commentCount > 0) {
                binding.tvCommentsCountSummary.setText(String.format(Locale.getDefault(), "%d comments", commentCount));
                binding.tvCommentsCountSummary.setVisibility(View.VISIBLE);
            } else {
                binding.tvCommentsCountSummary.setVisibility(View.GONE);
            }

            // Divider and Wrapper Alignment Consistency
            binding.divider.setVisibility((likesCount > 0 || commentCount > 0) ? View.VISIBLE : View.GONE);


            binding.btnLike.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onLikeClick(itemWithUser);
            });
            binding.btnLike.setOnLongClickListener(v -> {
                DialogHelper.showEmojiPicker(binding.btnLike, emoji -> {
                    if (interactionListener != null) interactionListener.onReactionClick(itemWithUser, emoji);
                });
                return true;
            });
            
            binding.layoutLikesSummaryClickable.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onViewLikesClick(item, itemWithUser.getReactorNames());
            });
            
            binding.tvLikesCount.setClickable(false);
            binding.tvReactionEmoji.setClickable(false);

            binding.tvCommentsCountSummary.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onCommentClick(item);
            });
            binding.btnComment.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onCommentClick(item);
            });
            binding.getRoot().setOnClickListener(v -> {
                if (itemClickListener != null) itemClickListener.onItemClick(itemWithUser);
            });
        }
    }
}
