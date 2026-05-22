package com.upreyvan.carti.ui.notifications;

import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemNotificationBinding;
import com.upreyvan.carti.model.Notification;
import com.upreyvan.carti.util.Utils;
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
              (binding, notification) -> {
                  binding.tvTitle.setText(notification.getTitle());
                  binding.tvDescription.setText(notification.getDescription());
                  binding.tvTime.setText(Utils.getTimeAgo(notification.getTimestamp()));

                  if (notification.getType() == Notification.Type.JOIN_REQUEST) {
                      binding.layoutActions.setVisibility(View.VISIBLE);
                  } else {
                      binding.layoutActions.setVisibility(View.GONE);
                  }
              });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<ItemNotificationBinding> holder, int position) {
        super.onBindViewHolder(holder, position);
        Notification notification = getItem(position);
        if (notification.getType() == Notification.Type.JOIN_REQUEST) {
            holder.binding.btnAccept.setOnClickListener(v -> {
                if (onAcceptListener != null) {
                    onAcceptListener.onAction(notification.getApplicantId());
                }
            });
            holder.binding.btnDeny.setOnClickListener(v -> {
                if (onDenyListener != null) {
                    onDenyListener.onAction(notification.getApplicantId());
                }
            });
        }
    }

    public void setNotifications(java.util.List<Notification> notifications) {
        submitList(notifications);
    }

    public void addNotificationAtTop(Notification notification) {
        java.util.List<Notification> currentList = new java.util.ArrayList<>(getCurrentList());
        currentList.add(0, notification);
        submitList(currentList);
    }

    public void clearAll() {
        submitList(Collections.emptyList());
    }
}