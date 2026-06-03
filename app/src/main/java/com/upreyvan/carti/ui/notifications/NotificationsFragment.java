package com.upreyvan.carti.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.FragmentNotificationsBinding;
import com.upreyvan.carti.model.Notification;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class NotificationsFragment extends BaseFragment<FragmentNotificationsBinding> {

    private NotificationAdapter adapter;
    private ApiHelper apiHelper;
    private PreferenceManager pref;
    private RealtimeRepository realtimeRepo;

    @Override
    protected FragmentNotificationsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentNotificationsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        pref = PreferenceManager.getInstance(requireContext());
        realtimeRepo = RealtimeRepository.getInstance(requireContext());
        
        pref.setHasNotifications(false);
        pref.setLastNotifCheck(com.upreyvan.carti.util.Utils.getCurrentTimestamp());
        
        setupToolbar();
        setupRecyclerView();

        loadAllNotifications();
        observeRealtimeChanges();
    }

    private void observeRealtimeChanges() {
        realtimeRepo.getNotificationStream().observe(getViewLifecycleOwner(), payload -> {
            if (payload != null) {
                String title = String.valueOf(payload.get("title"));
                String content = String.valueOf(payload.get("content"));
                
                Notification newNotif = new Notification(
                        title, 
                        content, 
                        System.currentTimeMillis(), 
                        Notification.Type.INFO, 
                        null
                );
                
                adapter.addNotificationAtTop(newNotif);
                getBinding().rvNotifications.scrollToPosition(0);
                checkEmptyState();
            }
        });

        if (pref.isAdmin()) {
            realtimeRepo.getUserUpdateStream().observe(getViewLifecycleOwner(), payload -> {
                if (payload != null) {
                    loadAllNotifications(); 
                }
            });
        }
    }

    private void loadAllNotifications() {
        List<Notification> allNotifications = new ArrayList<>();

        allNotifications.add(new Notification(
                "Welcome to Carti!",
                "Start tracking your family expenses and reach your goals together.",
                System.currentTimeMillis(),
                Notification.Type.INFO,
                null
        ));

        if (pref.isAdmin()) {
            apiHelper.getPendingMembers(new AppwriteManager.AppwriteCallback<>() {
                @Override
                public void onSuccess(DocumentList<Map<String, Object>> result) {
                    if (!isAdded()) return;
                    for (Document<Map<String, Object>> doc : result.getDocuments()) {
                        String userName = String.valueOf(doc.getData().get("username"));
                        String userId = doc.getId();
                        
                        allNotifications.add(new Notification(
                                getString(R.string.notif_join_request_title),
                                getString(R.string.notif_join_request_desc, userName),
                                System.currentTimeMillis(),
                                Notification.Type.JOIN_REQUEST,
                                userId
                        ));
                    }
                    loadFamilyAnnouncements(allNotifications);
                }

                @Override
                public void onError(Throwable error) {
                    loadFamilyAnnouncements(allNotifications);
                }
            });
        } else {
            loadFamilyAnnouncements(allNotifications);
        }
    }

    private void loadFamilyAnnouncements(List<Notification> allNotifications) {
        String familyId = pref.getFamilyId();
        if (familyId.isEmpty()) {
            updateUI(allNotifications);
            return;
        }

        apiHelper.getNotifications(familyId, new AppwriteManager.AppwriteCallback<DocumentList<Map<String, Object>>>() {
            @Override
            public void onSuccess(DocumentList<Map<String, Object>> result) {
                if (!isAdded()) return;
                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    String title = String.valueOf(doc.getData().get("title"));
                    String content = String.valueOf(doc.getData().get("content"));
                    long timestamp = 0;
                    try {
                        timestamp = com.upreyvan.carti.util.Utils.getMillisFromIso(String.valueOf(doc.getData().get("$createdAt")));
                    } catch (Exception ignored) {}

                    allNotifications.add(new Notification(
                            title,
                            content,
                            timestamp > 0 ? timestamp : System.currentTimeMillis(),
                            Notification.Type.INFO,
                            null
                    ));
                }
                updateUI(allNotifications);
            }

            @Override
            public void onError(Throwable error) {
                updateUI(allNotifications);
            }
        });
    }

    private void updateUI(List<Notification> list) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                adapter.setNotifications(list);
                checkEmptyState();
            });
        }
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.notifications_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
        
        if (pref.isAdmin()) {
            getBinding().layoutToolbar.btnAction.setVisibility(View.VISIBLE);
            getBinding().layoutToolbar.btnAction.setText(R.string.action_send_announcement);
            getBinding().layoutToolbar.btnAction.setOnClickListener(v -> {
                SendNotificationBottomSheetFragment bottomSheet = new SendNotificationBottomSheetFragment();
                bottomSheet.show(getChildFragmentManager(), "SendNotificationBottomSheet");
            });
        } else {
            getBinding().layoutToolbar.btnAction.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        adapter.setOnAcceptListener(this::approveMember);
        adapter.setOnDenyListener(this::rejectMember);
        getBinding().rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvNotifications.setAdapter(adapter);
    }

    private void approveMember(String userId) {
        apiHelper.approveJoinRequest(userId, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showToast(getString(R.string.msg_member_approved), ToastHelper.Status.SUCCESS);
                    loadAllNotifications();
                });
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showToast(error.getMessage(), ToastHelper.Status.ERROR));
            }
        });
    }

    private void rejectMember(String userId) {
        apiHelper.rejectJoinRequest(userId, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showToast(getString(R.string.msg_member_rejected), ToastHelper.Status.INFO);
                    loadAllNotifications();
                });
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showToast(error.getMessage(), ToastHelper.Status.ERROR));
            }
        });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
