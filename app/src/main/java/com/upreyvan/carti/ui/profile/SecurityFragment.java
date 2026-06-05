package com.upreyvan.carti.ui.profile;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentSecurityBinding;
import com.upreyvan.carti.ui.auth.LoginActivity;

public class SecurityFragment extends BaseFragment<FragmentSecurityBinding> {

    private AppwriteManager appwriteManager;
    private ApiHelper apiHelper;
    private PreferenceManager pref;

    @Override
    protected FragmentSecurityBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentSecurityBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        appwriteManager = AppwriteManager.getInstance(requireContext());
        apiHelper = new ApiHelper(requireContext());
        pref = PreferenceManager.getInstance(requireContext());
        
        setupToolbar();
        setupListeners();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.security_title);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupListeners() {
        getBinding().btnResetPassword.setOnClickListener(v -> showChangePasswordDialog());
        getBinding().btnSignOutAll.setOnClickListener(v -> signOutAllDevices());
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
                showToast(R.string.err_required, com.upreyvan.carti.util.UiHelper.Status.WARNING);
                return;
            }

            if (newPass.length() < 8) {
                showToast(R.string.msg_password_short, com.upreyvan.carti.util.UiHelper.Status.WARNING);
                return;
            }

            dialog.dismiss();
            performUpdatePassword(newPass, oldPass);
        });

        dialog.show();
    }

    private void performUpdatePassword(String newPassword, String oldPassword) {
        getBinding().btnResetPassword.setEnabled(false);
        appwriteManager.updatePassword(newPassword, oldPassword, new AppwriteManager.AppwriteCallback<io.appwrite.models.User<java.util.Map<String, Object>>>() {
            @Override
            public void onSuccess(io.appwrite.models.User<java.util.Map<String, Object>> result) {
                if (!isAdded()) return;
                getBinding().btnResetPassword.setEnabled(true);
                showToast(R.string.msg_password_updated, com.upreyvan.carti.util.UiHelper.Status.SUCCESS);
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                getBinding().btnResetPassword.setEnabled(true);
                showToast("Error: " + error.getMessage(), com.upreyvan.carti.util.UiHelper.Status.ERROR);
            }
        });
    }

    private void signOutAllDevices() {
        showLoading(true, "Signing out all devices...");
        getBinding().btnSignOutAll.setEnabled(false);

        appwriteManager.logoutAll(new AppwriteManager.AppwriteCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                finishSignOutAll();
            }

            @Override
            public void onError(Throwable error) {
                finishSignOutAll();
            }
        });
    }

    private void finishSignOutAll() {
        if (!isAdded()) return;
        pref.clear();
        showLoading(false);
        showToast(R.string.msg_sign_out_all_success, com.upreyvan.carti.util.UiHelper.Status.SUCCESS);
        navigateToLogin();
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.section_delete_account)
                .setMessage(R.string.msg_delete_account_confirm)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_confirm_delete, (dialog, which) -> performDeleteAccount())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performDeleteAccount() {
        getBinding().btnDeleteAccount.setEnabled(false);
        apiHelper.deleteAccount(new AppwriteManager.AppwriteCallback<java.util.Map<String, Object>>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> result) {
                if (!isAdded()) return;
                pref.clear();
                showToast(R.string.msg_account_deleted, com.upreyvan.carti.util.UiHelper.Status.SUCCESS);
                navigateToLogin();
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                getBinding().btnDeleteAccount.setEnabled(true);
                showToast("Error: " + error.getMessage(), com.upreyvan.carti.util.UiHelper.Status.ERROR);
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
