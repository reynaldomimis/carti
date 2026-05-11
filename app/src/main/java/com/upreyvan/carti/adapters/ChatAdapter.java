package com.upreyvan.carti.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.upreyvan.carti.databinding.ItemChatLeftBinding;
import com.upreyvan.carti.databinding.ItemChatRightBinding;
import com.upreyvan.carti.model.ChatMessage;

public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return oldItem.getMessage().equals(newItem.getMessage()) && oldItem.getTime().equals(newItem.getTime());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return oldItem.getMessage().equals(newItem.getMessage());
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isMe() ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_ME) {
            ItemChatRightBinding binding = ItemChatRightBinding.inflate(inflater, parent, false);
            return new ChatViewHolder(binding);
        } else {
            ItemChatLeftBinding binding = ItemChatLeftBinding.inflate(inflater, parent, false);
            return new ChatViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        if (holder.binding instanceof ItemChatRightBinding) {
            ItemChatRightBinding b = (ItemChatRightBinding) holder.binding;
            b.tvSenderName.setText(message.getSenderName());
            b.tvMessage.setText(message.getMessage());
            b.tvTime.setText(message.getTime());
        } else {
            ItemChatLeftBinding b = (ItemChatLeftBinding) holder.binding;
            b.tvSenderName.setText(message.getSenderName());
            b.tvMessage.setText(message.getMessage());
            b.tvTime.setText(message.getTime());
        }
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public final ViewBinding binding;

        public ChatViewHolder(ViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}