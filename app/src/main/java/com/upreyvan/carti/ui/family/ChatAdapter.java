package com.upreyvan.carti.ui.family;

import android.content.res.ColorStateList;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.repository.AiRepository;
import com.upreyvan.carti.databinding.ItemChatLeftBinding;
import com.upreyvan.carti.databinding.ItemChatRightBinding;
import com.upreyvan.carti.databinding.ItemChatSummaryBinding;
import com.upreyvan.carti.databinding.ItemChatThinkingBinding;
import com.upreyvan.carti.models.ChatMessage;
import com.upreyvan.carti.utils.AvatarHelper;
import com.upreyvan.carti.utils.Constants;
import com.upreyvan.carti.utils.Utils;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ChatAdapter extends BaseAdapter<ChatMessage, ViewBinding> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private static final int VIEW_TYPE_THINKING = 5;
    private static final int VIEW_TYPE_SUMMARY = 6;
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
                (inflater, parent) -> ItemChatLeftBinding.inflate(inflater, parent, false),
                (binding, item) -> {});
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.isShimmer()) return VIEW_TYPE_THINKING;
        if (message.isSummary()) return VIEW_TYPE_SUMMARY;
        return message.isMe() ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public ViewHolder<ViewBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewBinding binding = switch (viewType) {
            case VIEW_TYPE_ME -> ItemChatRightBinding.inflate(inflater, parent, false);
            case VIEW_TYPE_THINKING -> ItemChatThinkingBinding.inflate(inflater, parent, false);
            case VIEW_TYPE_SUMMARY -> ItemChatSummaryBinding.inflate(inflater, parent, false);
            default -> ItemChatLeftBinding.inflate(inflater, parent, false);
        };
        return new ViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ViewBinding> holder, int position) {
        ChatMessage message = getItem(position);
        if (message.isShimmer()) return;

        if (holder.binding instanceof ItemChatSummaryBinding b) {
            JSONObject data = message.getSummaryData();
            if (data != null) {
                b.tvPeriod.setText(data.optString("period", "Financial Summary"));
                
                double balance = data.optDouble("balance", -1);
                b.tvTotalBalance.setText(Utils.formatCurrency(balance));
                b.tvTotalBalance.setVisibility(balance >= 0 ? View.VISIBLE : View.GONE);
                b.tvBalanceLabel.setVisibility(balance >= 0 ? View.VISIBLE : View.GONE);

                double income = data.optDouble("income", -1);
                b.rowIncome.setVisibility(income >= 0 ? View.VISIBLE : View.GONE);
                b.tvIncome.setText(Utils.formatCurrency(income));

                double expense = data.optDouble("expense", -1);
                b.rowExpense.setVisibility(expense >= 0 ? View.VISIBLE : View.GONE);
                b.tvExpense.setText(Utils.formatCurrency(expense));

                double alloc = data.optDouble("allocation", -1);
                b.rowAlloc.setVisibility(alloc >= 0 ? View.VISIBLE : View.GONE);
                b.tvAlloc.setText(Utils.formatCurrency(alloc));
            }
        } else if (holder.binding instanceof ItemChatRightBinding b) {
            b.tvSenderName.setText(b.getRoot().getContext().getString(R.string.chat_sender_me));
            b.tvMessage.setText(message.getMessage());
            b.tvTime.setText(message.getTime());
        } else if (holder.binding instanceof ItemChatLeftBinding b) {
            if (message.isMe()) {
                b.tvSenderName.setText(b.getRoot().getContext().getString(R.string.chat_sender_me));
            } else {
                b.tvSenderName.setText(message.getSenderName());
            }
            
            if (message.isCanceled()) {
                b.tvMessage.setText(b.getRoot().getContext().getString(R.string.msg_canceled));
                b.tvMessage.setAlpha(0.5f);
            } else {
                b.tvMessage.setText(message.getMessage());
                b.tvMessage.setAlpha(1.0f);
            }

            b.tvTime.setText(message.getTime());
            
            AvatarHelper.loadUserAvatar(b.getRoot().getContext(), b.ivAvatar, message.getSenderName());

            if (message.isCancelable() && !message.isCanceled()) {
                b.layoutCancel.setVisibility(View.VISIBLE);
                String act = message.getPendingAction() != null ? message.getPendingAction().optString("action", "Action") : "Action";
                b.tvActionLabel.setText(act.replace("ADD_", "").replace("_", " "));
                startTimer(b, message);
                b.btnCancel.setOnClickListener(v -> {
                    cancelTimer(message);
                    message.setCanceled(true);
                    notifyItemChanged(holder.getBindingAdapterPosition());
                    if (cancelListener != null) cancelListener.onCancel(message, holder.getBindingAdapterPosition());
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
                    AiRepository.getInstance(b.getRoot().getContext())
                            .executeAction(message.getPendingAction());
                }
            }
        }.start();
        activeTimers.put(message, timer);
    }

    private void cancelTimer(ChatMessage message) {
        CountDownTimer timer = activeTimers.get(message);
        if (timer != null) {
            timer.cancel();
            activeTimers.remove(message);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder<ViewBinding> holder) {
        super.onViewRecycled(holder);
    }
}
