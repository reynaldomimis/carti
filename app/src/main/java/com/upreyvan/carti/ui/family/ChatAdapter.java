package com.upreyvan.carti.ui.family;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemChatLeftBinding;
import com.upreyvan.carti.databinding.ItemChatRightBinding;
import com.upreyvan.carti.databinding.ItemChatShimmerLeftBinding;
import com.upreyvan.carti.databinding.ItemChatShimmerRightBinding;
import com.upreyvan.carti.model.ChatMessage;

import java.util.HashMap;
import java.util.Map;

public class ChatAdapter extends BaseAdapter<ChatMessage, ViewBinding> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private static final int VIEW_TYPE_THINKING = 5;
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
        super(ChatMessage.DIFF_CALLBACK,
                (inflater, parent) -> {
                  return ItemChatLeftBinding.inflate(inflater, parent, false);
                },
                (binding, item) -> {
              });
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.isShimmer()) {
            return VIEW_TYPE_THINKING;
        }
        return message.isMe() ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public ViewHolder<ViewBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewBinding binding;
        switch (viewType) {
            case VIEW_TYPE_ME:
                binding = ItemChatRightBinding.inflate(inflater, parent, false);
                break;
            case VIEW_TYPE_THINKING:
                binding = com.upreyvan.carti.databinding.ItemChatThinkingBinding.inflate(inflater, parent, false);
                break;
            default:
                binding = ItemChatLeftBinding.inflate(inflater, parent, false);
                break;
        }
        return new ViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ViewBinding> holder, int position) {
        ChatMessage message = getItem(position);
        if (message.isShimmer()) {
            // Thinking view uses indeterminate progress indicator, no binding needed
            return;
        }

        if (holder.binding instanceof ItemChatRightBinding) {
            ItemChatRightBinding b = (ItemChatRightBinding) holder.binding;
            b.tvSenderName.setText(b.getRoot().getContext().getString(R.string.chat_sender_me));
            b.tvMessage.setText(message.getMessage());
            b.tvTime.setText(message.getTime());
        } else if (holder.binding instanceof ItemChatLeftBinding) {
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
    public void onViewRecycled(@NonNull ViewHolder<ViewBinding> holder) {
        super.onViewRecycled(holder);
        cancelTimer(holder.getAdapterPosition());
    }
}
