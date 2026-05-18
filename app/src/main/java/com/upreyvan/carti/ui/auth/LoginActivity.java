package com.upreyvan.carti.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.ActivityLoginBinding;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;

public class LoginActivity extends BaseActivity<ActivityLoginBinding> {

    @Override
    protected ActivityLoginBinding inflateBinding(LayoutInflater inflater) {
        return ActivityLoginBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleResetPasswordIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleResetPasswordIntent(intent);
    }

    private void handleResetPasswordIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;

        boolean isCarti = "carti".equals(data.getScheme())
                && "reset-password".equals(data.getHost());
        
        boolean isVercel = ("http".equals(data.getScheme()) || "https".equals(data.getScheme()))
                && "mintyai.vercel.app".equals(data.getHost())
                && "/reset-password".equals(data.getPath());

        if (!isCarti && !isVercel) return;

        String userId = data.getQueryParameter("userId");
        String secret = data.getQueryParameter("secret");

        if (Validator.areNotEmpty(userId, secret)) {
            Intent resetIntent = new Intent(this, ResetPasswordActivity.class);
            resetIntent.putExtra(ResetPasswordActivity.EXTRA_USER_ID, userId);
            resetIntent.putExtra(ResetPasswordActivity.EXTRA_SECRET, secret);
            startActivity(resetIntent);
        } else {
            showToast(
                    getString(R.string.err_generic, "Invalid or missing reset parameters"),
                    ToastHelper.Status.ERROR
            );
        }
    }
}