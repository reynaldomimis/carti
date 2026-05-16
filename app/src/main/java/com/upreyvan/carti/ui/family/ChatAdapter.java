package com.upreyvan.carti.ui.family;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.ItemChatLeftBinding;
import com.upreyvan.carti.databinding.ItemChatRightBinding;
import com.upreyvan.carti.model.ChatMessage;

import android.os.CountDownTimer;
import java.util.HashMap;
import java.util.Map;

public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private static final int VIEW_TYPE_SHIMMER_ME = 3;
    private static final int VIEW_TYPE_SHIMMER_OTHER = 4;
    private static final int TIMER_DURATION = 5000;

    private OnCancelListener cancelListener;
    private final Map<Integer, CountDownTimer> activeTimers = new HashMap<>();

    public interface OnCancelListener {
        void onCancel(ChatMessage message, int position);
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }

    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                if (oldItem.isShimmer() || newItem.isShimmer()) return false;
                if (oldItem.getId() != null && newItem.getId() != null) {
                    return oldItem.getId().equals(newItem.getId());
                }
                return oldItem.getMessage().equals(newItem.getMessage()) && oldItem.getTime().equals(newItem.getTime());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return oldItem.getMessage().equals(newItem.getMessage()) 
                        && oldItem.isCanceled() == newItem.isCanceled();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.isShimmer()) {
            return message.isMe() ? VIEW_TYPE_SHIMMER_ME : VIEW_TYPE_SHIMMER_OTHER;
        }
        return message.isMe() ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_ME:
                return new ChatViewHolder(ItemChatRightBinding.inflate(inflater, parent, false));
            case VIEW_TYPE_SHIMMER_ME:
                return new ChatViewHolder(com.upreyvan.carti.databinding.ItemChatShimmerRightBinding.inflate(inflater, parent, false));
            case VIEW_TYPE_SHIMMER_OTHER:
                return new ChatViewHolder(com.upreyvan.carti.databinding.ItemChatShimmerLeftBinding.inflate(inflater, parent, false));
            default:
                return new ChatViewHolder(ItemChatLeftBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        if (message.isShimmer()) {
            if (holder.binding instanceof com.upreyvan.carti.databinding.ItemChatShimmerRightBinding) {
                ((com.upreyvan.carti.databinding.ItemChatShimmerRightBinding) holder.binding).shimmerLayout.startShimmer();
            } else if (holder.binding instanceof com.upreyvan.carti.databinding.ItemChatShimmerLeftBinding) {
                ((com.upreyvan.carti.databinding.ItemChatShimmerLeftBinding) holder.binding).shimmerLayout.startShimmer();
            }
            return;
        }

        if (holder.binding instanceof ItemChatRightBinding) {
            ItemChatRightBinding b = (ItemChatRightBinding) holder.binding;
            b.tvSenderName.setText(message.getSenderName());
            b.tvMessage.setText(message.getMessage());
            b.tvTime.setText(message.getTime());
        } else {
            ItemChatLeftBinding b = (ItemChatLeftBinding) holder.binding;
            b.tvSenderName.setText(message.getSenderName());
            
            if (message.isCanceled()) {
                b.tvMessage.setText(b.getRoot().getContext().getString(R.string.msg_canceled));
                b.tvMessage.setAlpha(0.5f);
            } else {
                b.tvMessage.setText(message.getMessage());
                b.tvMessage.setAlpha(1.0f);
            }

            b.tvTime.setText(message.getTime());
            
            if (message.getImageResId() != 0) {
                b.ivAvatar.setImageResource(message.getImageResId());
            } else {
                b.ivAvatar.setImageResource(R.drawable.ic_person);
            }

            // Handle Cancel Button and Timer
            if (message.isCancelable() && !message.isCanceled()) {
                b.layoutCancel.setVisibility(View.VISIBLE);
                startTimer(b, message, holder.getAdapterPosition());
                b.btnCancel.setOnClickListener(v -> {
                    cancelTimer(holder.getAdapterPosition());
                    message.setCanceled(true);
                    notifyItemChanged(holder.getAdapterPosition());
                    if (cancelListener != null) {
                        cancelListener.onCancel(message, holder.getAdapterPosition());
                    }
                });
            } else {
                b.layoutCancel.setVisibility(View.GONE);
                cancelTimer(holder.getAdapterPosition());
            }
        }
    }

    private void startTimer(ItemChatLeftBinding b, ChatMessage message, int position) {
        cancelTimer(position);
        CountDownTimer timer = new CountDownTimer(TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                b.tvTimer.setText(b.getRoot().getContext().getString(R.string.timer_format, (int) (millisUntilFinished / 1000) + 1));
            }

            @Override
            public void onFinish() {
                b.layoutCancel.setVisibility(View.GONE);
            }
        }.start();
        activeTimers.put(position, timer);
    }

    private void cancelTimer(int position) {
        if (activeTimers.containsKey(position)) {
            activeTimers.get(position).cancel();
            activeTimers.remove(position);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ChatViewHolder holder) {
        super.onViewRecycled(holder);
        cancelTimer(holder.getAdapterPosition());
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public final ViewBinding binding;

        public ChatViewHolder(ViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}