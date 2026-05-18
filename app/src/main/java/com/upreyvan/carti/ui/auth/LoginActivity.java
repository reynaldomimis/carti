package com.upreyvan.carti.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.upreyvan.carti.R;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.ActivityLoginBinding;

public class LoginActivity extends BaseActivity<ActivityLoginBinding> {

    private AppwriteManager appwriteManager;

    @Override
    protected ActivityLoginBinding inflateBinding(LayoutInflater inflater) {
        return ActivityLoginBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appwriteManager = AppwriteManager.getInstance(this);

        handleResetPasswordIntent(getIntent());
    }

    private void handleResetPasswordIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            boolean isOldCustomScheme = "carti".equals(data.getScheme()) && "reset-password".equals(data.getHost());
            boolean isNewHttpsScheme = "https".equals(data.getScheme()) && "mintyai.vercel.app".equals(data.getHost()) && data.getPath() != null && data.getPath().startsWith("/reset-password");

            if (isOldCustomScheme || isNewHttpsScheme) {
                String userId = data.getQueryParameter("userId");
                String secret = data.getQueryParameter("secret");

                if (userId != null && secret != null) {
                    showNewPasswordDialog(userId, secret);
                }
            }
        }
    }

    private void showNewPasswordDialog(String userId, String secret) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_set_new_password);

        final EditText input = new EditText(this);
        input.setHint(R.string.hint_new_password);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(40, 20, 40, 20);
        input.setLayoutParams(lp);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPassword = input.getText().toString();
            if (newPassword.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            updatePassword(userId, secret, newPassword);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updatePassword(String userId, String secret, String password) {
        appwriteManager.updatePasswordRecovery(userId, secret, password, new AppwriteManager.AppwriteCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(LoginActivity.this, "Password updated successfully! Please login.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(LoginActivity.this, "Error updating password: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
