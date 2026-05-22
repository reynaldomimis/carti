package com.upreyvan.carti.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.LayoutSendNotificationBottomSheetBinding;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;

import java.util.Map;

public class SendNotificationBottomSheetFragment extends BottomSheetDialogFragment {

    private LayoutSendNotificationBottomSheetBinding binding;
    private ApiHelper apiHelper;
    private PreferenceManager pref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LayoutSendNotificationBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiHelper = new ApiHelper(requireContext());
        pref = new PreferenceManager(requireContext());

        binding.btnSend.setOnClickListener(v -> sendNotification());
    }

    private void sendNotification() {
        String title = binding.etTitle.getText().toString().trim();
        String content = binding.etContent.getText().toString().trim();

        if (title.isEmpty()) {
            binding.tilTitle.setError(getString(R.string.err_required));
            return;
        }
        binding.tilTitle.setError(null);

        if (content.isEmpty()) {
            binding.tilContent.setError(getString(R.string.err_required));
            return;
        }
        binding.tilContent.setError(null);

        binding.btnSend.setEnabled(false);
        binding.btnSend.setText(R.string.msg_sending);

        apiHelper.sendAnnouncement(
                title,
                content,
                new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                ToastHelper.show(requireContext(), "Announcement sent!", ToastHelper.Status.SUCCESS);
                                dismiss();
                            });
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                binding.btnSend.setEnabled(true);
                                binding.btnSend.setText(R.string.action_send_announcement);
                                ToastHelper.show(requireContext(), error.getMessage(), ToastHelper.Status.ERROR);
                            });
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
