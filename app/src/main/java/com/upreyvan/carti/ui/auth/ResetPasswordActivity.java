package com.upreyvan.carti.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.remote.AppwriteManager.AppwriteCallback;
import com.upreyvan.carti.databinding.ActivityResetPasswordBinding;
import com.upreyvan.carti.util.Constants.ErrorCodes;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;

public class ResetPasswordActivity extends BaseActivity<ActivityResetPasswordBinding> {

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_SECRET = "extra_secret";

    private String userId;
    private String secret;

    @Override
    protected ActivityResetPasswordBinding inflateBinding(LayoutInflater inflater) {
        return ActivityResetPasswordBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        secret = getIntent().getStringExtra(EXTRA_SECRET);

        if (Validator.isEmpty(userId) || Validator.isEmpty(secret)) {
            showToast(R.string.err_invalid_reset_link, ToastHelper.Status.ERROR);
            finish();
            return;
        }

        setupUI();
        setupKeyboardHandling();
    }

    private void setupKeyboardHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(getBinding().getRoot(), (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeHeight > 0 ? imeHeight : systemBarsBottom);
            return insets;
        });
    }

    private void setupUI() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.title_set_new_password);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());

        getBinding().btnReset.setOnClickListener(v -> handleReset());
    }

    private void handleReset() {
        String newPassword = getBinding().etNewPassword.getText().toString().trim();
        String confirmPassword = getBinding().etConfirmPassword.getText().toString().trim();

        if (!Validator.isValidPassword(newPassword)) {
            getBinding().tilNewPassword.setError(getString(R.string.msg_password_short));
            return;
        } else {
            getBinding().tilNewPassword.setError(null);
        }

        if (!newPassword.equals(confirmPassword)) {
            getBinding().tilConfirmPassword.setError(getString(R.string.err_password_mismatch));
            return;
        } else {
            getBinding().tilConfirmPassword.setError(null);
        }

        getBinding().btnReset.setEnabled(false);
        showToast(R.string.msg_updating_password, ToastHelper.Status.INFO);

        AppwriteManager.getInstance(this).updatePasswordRecovery(userId, secret, newPassword, new AppwriteCallback<>() {
            @Override public void onSuccess(Object result) {
                showToast(R.string.msg_password_updated, ToastHelper.Status.SUCCESS);
                finish();
            }
            @Override public void onError(Throwable error) {
                getBinding().btnReset.setEnabled(true);
                String message = error.getMessage();
                if (message != null && message.contains("similar to your previous password")) {
                    showToast(ErrorCodes.PASSWORD_RECENTLY_USED, ToastHelper.Status.ERROR);
                } else {
                    showToast("Error: " + (message != null ? message : "Unknown error"), ToastHelper.Status.ERROR);
                }
            }
        });
    }
}
