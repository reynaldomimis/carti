package com.upreyvan.carti.ui.notifications;

import android.view.View;
import androidx.annotation.NonNull;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemNotificationBinding;
import com.upreyvan.carti.models.Notification;
import com.upreyvan.carti.utils.Utils;
import java.util.Collections;

public class NotificationAdapter extends BaseAdapter<Notification, ItemNotificationBinding> {

    public interface OnNotificationActionListener {
        void onAction(String applicantId);
    }

    private OnNotificationActionListener onAcceptListener;
    private OnNotificationActionListener onDenyListener;

    public void setOnAcceptListener(OnNotificationActionListener listener) {
        this.onAcceptListener = listener;
    }

    public void setOnDenyListener(OnNotificationActionListener listener) {
        this.onDenyListener = listener;
    }

    public NotificationAdapter() {
        super(Notification.DIFF_CALLBACK,
              (inflater, parent) -> ItemNotificationBinding.inflate(inflater, parent, false),
              (binding, notification, position, count) -> {
                  binding.tvTitle.setText(notification.getTitle());
                  binding.tvDescription.setText(notification.getContent());
                  binding.tvTime.setText(Utils.getTimeAgo(notification.getTimestampMillis()));

                  boolean isJoinRequest = "family".equalsIgnoreCase(notification.getType()) && 
                                        "New Join Request".equalsIgnoreCase(notification.getTitle());
                  
                  binding.layoutActions.setVisibility(isJoinRequest ? View.VISIBLE : View.GONE);
                  
                  // Visual feedback for unread status
                  binding.getRoot().setAlpha(notification.isUnread() ? 1.0f : 0.6f);
              });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ItemNotificationBinding> holder, int position) {
        super.onBindViewHolder(holder, position);
        Notification notification = getItem(position);

        boolean isJoinRequest = "family".equalsIgnoreCase(notification.getType()) && 
                              "New Join Request".equalsIgnoreCase(notification.getTitle());

        if (isJoinRequest) {
            holder.binding.btnAccept.setOnClickListener(v -> {
                if (onAcceptListener != null) {
                    // Extracting ID from content or metadata if available. 
                    // Currently relying on the parent view to handle specific join request logic if needed.
                }
            });
        }
    }

    public void setNotifications(java.util.List<Notification> notifications) {
        submitList(notifications);
    }
}