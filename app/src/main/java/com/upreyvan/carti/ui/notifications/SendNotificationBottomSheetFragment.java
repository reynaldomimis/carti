package com.upreyvan.carti.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.databinding.LayoutSendNotificationBottomSheetBinding;
import com.upreyvan.carti.ui.bills.BillsViewModel;
import com.upreyvan.carti.utils.ToastHelper;
import com.upreyvan.carti.utils.UiHelper;

public class SendNotificationBottomSheetFragment extends BottomSheetDialogFragment {

    private LayoutSendNotificationBottomSheetBinding binding;
    private BillsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LayoutSendNotificationBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BillsViewModel.class);

        binding.btnSend.setOnClickListener(v -> sendNotification());
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                ToastHelper.show(requireContext(), "Announcement sent!", UiHelper.Status.SUCCESS);
                dismiss();
            }
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnSend.setEnabled(!loading);
            binding.btnSend.setText(loading ? R.string.msg_sending : R.string.action_send_announcement);
        });
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

        viewModel.saveBill(title, content, 0.0, "Announcement");
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
