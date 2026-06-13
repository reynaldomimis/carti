package com.upreyvan.carti.ui.notifications;

import android.view.View;
import androidx.annotation.NonNull;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemNotificationBinding;
import com.upreyvan.carti.models.Notification;
import com.upreyvan.carti.utils.Utils;
import java.util.Collections;

public class NotificationAdapter extends BaseAdapter<Notification, ItemNotificationBinding> {

    public interface OnNotificationActionListener {
        void onAction(Notification notification);
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
                  
                  String description = notification.getContent();
                  if (notification.getNotes() != null && !notification.getNotes().trim().isEmpty()) {
                      description += "\n" + notification.getNotes().trim();
                  }
                  binding.tvDescription.setText(description);
                  binding.tvTime.setText(Utils.getTimeAgo(notification.getTimestampMillis()));

                  // Handle long descriptions (like those with extra details)
                  binding.tvDescription.setMaxLines(Integer.MAX_VALUE);
                  binding.tvDescription.setEllipsize(null);

                  boolean isJoinRequest = "family".equalsIgnoreCase(notification.getType()) && 
                                        "New Join Request".equalsIgnoreCase(notification.getTitle());
                  
                  binding.layoutActions.setVisibility(isJoinRequest ? View.VISIBLE : View.GONE);
                  
                  boolean isUnread = notification.isUnread();
                  boolean isPaid = "paid".equalsIgnoreCase(notification.getStatus());

                  // 1. Dot Logic (Green/Gray)
                  binding.viewUnreadDot.setVisibility(isPaid ? View.GONE : View.VISIBLE);
                  
                  int dotColor = isUnread ? R.color.carti_primary_green : R.color.nav_inactive;
                  binding.viewUnreadDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                          androidx.core.content.ContextCompat.getColor(binding.getRoot().getContext(), dotColor)
                  ));

                  // 2. Opacity Logic (Messenger-style)
                  binding.getRoot().setAlpha(isUnread ? 1.0f : 0.7f);
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
                    onAcceptListener.onAction(notification);
                }
            });
            holder.binding.btnDeny.setOnClickListener(v -> {
                if (onDenyListener != null) {
                    onDenyListener.onAction(notification);
                }
            });
        }
    }

    public void setNotifications(java.util.List<Notification> notifications) {
        submitList(notifications);
    }
}