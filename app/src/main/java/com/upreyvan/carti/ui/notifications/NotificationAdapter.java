package com.upreyvan.carti.ui.notifications;

import android.view.View;
import android.widget.Toast;
import com.upreyvan.carti.base.BaseAdapter;
import com.upreyvan.carti.databinding.ItemNotificationBinding;
import com.upreyvan.carti.model.Notification;
import com.upreyvan.carti.util.Utils;
import java.util.Collections;

public class NotificationAdapter extends BaseAdapter<Notification, ItemNotificationBinding> {

    public NotificationAdapter() {
        super(Notification.DIFF_CALLBACK,
              (inflater, parent) -> ItemNotificationBinding.inflate(inflater, parent, false),
              (binding, notification) -> {
                  binding.tvTitle.setText(notification.getTitle());
                  binding.tvDescription.setText(notification.getDescription());
                  binding.tvTime.setText(Utils.getTimeAgo(notification.getTimestamp()));

                  if (notification.getType() == Notification.Type.JOIN_REQUEST) {
                      binding.layoutActions.setVisibility(View.VISIBLE);
                      binding.btnAccept.setOnClickListener(v -> {
                          Toast.makeText(v.getContext(), "Accepted " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                          // Logic to handle acceptance
                      });
                      binding.btnDeny.setOnClickListener(v -> {
                          Toast.makeText(v.getContext(), "Denied " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                          // Logic to handle denial
                      });
                  } else {
                      binding.layoutActions.setVisibility(View.GONE);
                  }
              });
    }

    public void setNotifications(java.util.List<Notification> notifications) {
        submitList(notifications);
    }

    public void clearAll() {
        submitList(Collections.emptyList());
    }
}