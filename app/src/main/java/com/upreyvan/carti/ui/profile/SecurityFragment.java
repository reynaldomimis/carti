package com.upreyvan.carti.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentSecurityBinding;
import com.upreyvan.carti.ui.auth.LoginActivity;
import com.upreyvan.carti.utils.UiHelper;

public class SecurityFragment extends BaseFragment<FragmentSecurityBinding> {

    private ProfileViewModel viewModel;

    @Override
    protected FragmentSecurityBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentSecurityBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        setupToolbar();
        setupListeners();
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getLogoutSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) navigateToLogin();
        });
        
        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) showToast(msg, UiHelper.Status.SUCCESS);
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) showToast(err, UiHelper.Status.ERROR);
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> showLoading(loading));
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.security_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupListeners() {
        getBinding().btnResetPassword.setOnClickListener(v -> showChangePasswordDialog());
        getBinding().btnSignOutAll.setOnClickListener(v -> viewModel.logoutAll());
        getBinding().btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showChangePasswordDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);

        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        View btnUpdate = dialogView.findViewById(R.id.btn_update);

        btnUpdate.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                showToast(R.string.err_required, com.upreyvan.carti.utils.UiHelper.Status.WARNING);
                return;
            }

            if (newPass.length() < 8) {
                showToast(R.string.msg_password_short, com.upreyvan.carti.utils.UiHelper.Status.WARNING);
                return;
            }

            dialog.dismiss();
            viewModel.updatePassword(newPass, oldPass);
        });

        dialog.show();
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.section_delete_account)
                .setMessage(R.string.msg_delete_account_confirm)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_confirm_delete, (dialog, which) -> viewModel.deleteAccount())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
