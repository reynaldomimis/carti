package com.upreyvan.carti.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.managers.PreferenceManager;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentNotificationsBinding;
import com.upreyvan.carti.models.Notification;
import com.upreyvan.carti.utils.UiHelper;

public class NotificationsFragment extends BaseFragment<FragmentNotificationsBinding> {

    private NotificationAdapter adapter;
    private NotificationViewModel viewModel;
    private PreferenceManager pref;

    @Override
    protected FragmentNotificationsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentNotificationsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        pref = PreferenceManager.getInstance(requireContext());
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(false);
        }

        pref.setHasNotifications(false);
        pref.setLastNotifCheck(com.upreyvan.carti.utils.Utils.getCurrentTimestamp());
        
        setupToolbar();
        setupRecyclerView();
        observeViewModel();
        observeRealtimeChanges();
        
        viewModel.loadAll();
    }

    private void observeViewModel() {
        viewModel.getNotifications().observe(getViewLifecycleOwner(), list -> {
            adapter.setNotifications(list);
            checkEmptyState();
        });
        
        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                showToast(msg, UiHelper.Status.SUCCESS);
                viewModel.clearActionSuccess();
            }
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) showToast(err, UiHelper.Status.ERROR);
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::showLoading);
    }

    private void observeRealtimeChanges() {
        RealtimeRepository.getInstance(requireContext()).getNotificationStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                viewModel.loadAll();
            }
        });

        if (pref.isAdmin()) {
            RealtimeRepository.getInstance(requireContext()).getUserUpdateStream().observe(getViewLifecycleOwner(), payload -> {
                if (payload != null) {
                    viewModel.loadAll(); 
                }
            });
        }
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.notifications_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(com.upreyvan.carti.utils.Constants.Navigation.HOME);
            }
        });
        
        if (pref.isAdmin()) {
            getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
            getBinding().layoutToolbar.btnAction.setText(R.string.action_send_announcement);
            getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
                SendNotificationBottomSheetFragment bottomSheet = new SendNotificationBottomSheetFragment();
                bottomSheet.show(getChildFragmentManager(), "SendNotificationBottomSheet");
            });
        } else {
            getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
            getBinding().layoutToolbar.btnAction.setText("Mark all as read");
            getBinding().layoutToolbar.btnAction.setOnClickListener(v -> viewModel.markAllAsRead());
        }
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        adapter.setOnAcceptListener(notification -> {
            viewModel.approveMember(notification.getCategory());
            viewModel.markAsRead(notification.getId());
        });
        adapter.setOnDenyListener(notification -> {
            viewModel.rejectMember(notification.getCategory());
            viewModel.markAsRead(notification.getId());
        });
        adapter.setOnItemClickListener(notification -> {
            if (notification.isUnread()) {
                viewModel.markAsRead(notification.getId());
            }
        });
        getBinding().rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvNotifications.setAdapter(adapter);
    }

    private void checkEmptyState() {
        if (adapter.getItemCount() == 0) {
            getBinding().tvEmpty.setVisibility(View.VISIBLE);
            getBinding().rvNotifications.setVisibility(View.GONE);
        } else {
            getBinding().tvEmpty.setVisibility(View.GONE);
            getBinding().rvNotifications.setVisibility(View.VISIBLE);
        }
    }
}
