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
import com.upreyvan.carti.model.ChatMessage;
import com.upreyvan.carti.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class ChatAdapter extends BaseAdapter<ChatMessage, ViewBinding> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private static final int VIEW_TYPE_THINKING = 5;
    private static final int TIMER_DURATION = 10000;

    private OnCancelListener cancelListener;
    private final Map<ChatMessage, CountDownTimer> activeTimers = new HashMap<>();

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
            
            boolean isAi = Constants.Roles.AI_NAME.equalsIgnoreCase(message.getSenderName()) 
                    || Constants.Roles.AI_ID.equals(message.getSenderId());

            if (isAi) {
                b.ivAvatar.setImageResource(R.drawable.ai_holder);
                b.ivAvatar.setImageTintList(null);
            } else {
                b.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.gray)));
                if (message.getImageResId() != 0) {
                    b.ivAvatar.setImageResource(message.getImageResId());
                } else {
                    b.ivAvatar.setImageResource(R.drawable.ic_person);
                }
            }


            if (message.isCancelable() && !message.isCanceled()) {
                b.layoutCancel.setVisibility(View.VISIBLE);
                startTimer(b, message);
                b.btnCancel.setOnClickListener(v -> {
                    cancelTimer(message);
                    message.setCanceled(true);
                    notifyItemChanged(holder.getAdapterPosition());
                    if (cancelListener != null) {
                        cancelListener.onCancel(message, holder.getAdapterPosition());
                    }
                });
            } else {
                b.layoutCancel.setVisibility(View.GONE);
                cancelTimer(message);
            }
        }
    }

    private void startTimer(ItemChatLeftBinding b, ChatMessage message) {
        cancelTimer(message);
        CountDownTimer timer = new CountDownTimer(TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                b.tvTimer.setText(b.getRoot().getContext().getString(R.string.timer_format, (int) (millisUntilFinished / 1000) + 1));
            }

            @Override
            public void onFinish() {
                b.layoutCancel.setVisibility(View.GONE);
                if (!message.isCanceled() && message.getPendingAction() != null) {
                    com.upreyvan.carti.data.repository.AiRepository.getInstance(b.getRoot().getContext())
                            .executeAction(message.getPendingAction());
                }
            }
        }.start();
        activeTimers.put(message, timer);
    }

    private void cancelTimer(ChatMessage message) {
        if (activeTimers.containsKey(message)) {
            activeTimers.get(message).cancel();
            activeTimers.remove(message);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder<ViewBinding> holder) {
        super.onViewRecycled(holder);
        // We don't necessarily want to cancel the timer on recycle, 
        // but we should clear references if needed. 
        // Actually, for a 10s timer, it's better to let it run in background if not canceled.
    }
}
