package com.upreyvan.carti.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.R;
import com.upreyvan.carti.ui.notifications.NotificationAdapter;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentNotificationsBinding;
import com.upreyvan.carti.model.Notification;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends BaseFragment<FragmentNotificationsBinding> {

    private NotificationAdapter adapter;

    @Override
    protected FragmentNotificationsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentNotificationsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerView();
        loadDummyData();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.notifications_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
        
        getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnAction.setText(R.string.btn_clear_all);
        getBinding().layoutToolbar.btnAction.setIconResource(0); // Clear icon if any
        getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
            adapter.clearAll();
            checkEmptyState();
        });
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        getBinding().rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvNotifications.setAdapter(adapter);
    }

    private void loadDummyData() {
        List<Notification> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        list.add(new Notification("Maria (Spouse)", "wants to join your family group.", now, Notification.Type.JOIN_REQUEST));
        list.add(new Notification(getString(R.string.mock_notif_title_1), getString(R.string.mock_notif_desc_1), now - 3600000)); // 1 hour ago
        list.add(new Notification(getString(R.string.mock_notif_title_2), getString(R.string.mock_notif_desc_2), now - 86400000)); // 1 day ago
        list.add(new Notification(getString(R.string.mock_notif_title_3), getString(R.string.mock_notif_desc_3), now - 172800000)); // 2 days ago
        
        adapter.setNotifications(list);
        checkEmptyState();
    }

    private void checkEmptyState() {
        if (adapter.getItemCount() == 0) {
            getBinding().tvEmpty.setVisibility(View.VISIBLE);
            getBinding().rvNotifications.setVisibility(View.GONE);
            getBinding().layoutToolbar.btnAction.setVisibility(View.GONE);
        } else {
            getBinding().tvEmpty.setVisibility(View.GONE);
            getBinding().rvNotifications.setVisibility(View.VISIBLE);
            getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
        }
    }
}