package com.upreyvan.carti.base;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewbinding.ViewBinding;

import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.util.DialogHelper;
import com.upreyvan.carti.util.LoadingDialog;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;

public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    private VB binding;
    private LoadingDialog loadingDialog;

    protected abstract VB inflateBinding(LayoutInflater inflater);

    protected void showToast(String message, ToastHelper.Status status) {
        ToastHelper.show(this, message, status);
    }

    protected void showToast(int resId, ToastHelper.Status status) {
        ToastHelper.show(this, resId, status);
    }

    protected void showLoading(boolean isLoading) {
        showLoading(isLoading, getString(com.upreyvan.carti.R.string.label_saving));
    }

    protected void showLoading(boolean isLoading, String message) {
        if (isLoading) {
            if (loadingDialog == null) {
                loadingDialog = new LoadingDialog(this);
            }
            loadingDialog.setMessage(message);
            if (!loadingDialog.isShowing()) {
                loadingDialog.show();
            }
        } else {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());
        setupSystemUI();

        observeNotifications();
    }

    private void observeNotifications() {
        RealtimeRepository repo = RealtimeRepository.getInstance(this);

        repo.getNotificationStream().observe(this, payload -> {
            if (payload != null) {
                String title = String.valueOf(payload.get("title"));
                if (!title.isEmpty()) {
                    showToast("Announcement: " + title, ToastHelper.Status.SUCCESS);
                }
            }
        });

        repo.getUserUpdateStream().observe(this, payload -> {
            if (payload != null) {
                String userName = String.valueOf(payload.get("username"));
                showToast(userName + " wants to join your family!", ToastHelper.Status.INFO);
            }
        });

        repo.getTransactionStream().observe(this, payload -> {
            if (payload != null) {
                try {
                    double amount = Double.parseDouble(String.valueOf(payload.get("amount")));
                    String type = String.valueOf(payload.get("type"));
                    if ("Expense".equalsIgnoreCase(type) && amount > 1000) {
                        showToast("Large expense detected: ₱" + amount, ToastHelper.Status.WARNING);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected VB getBinding() {
        return binding;
    }

    protected boolean checkNetwork() {
        if (!Utils.isNetworkAvailable(this)) {
            DialogHelper.showNoInternetDialog(this);
            return false;
        }
        return true;
    }

    protected void executeWithNetwork(Runnable action) {
        if (checkNetwork()) {
            action.run();
        }
    }

    private void setupSystemUI() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        View decorView = window.getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, decorView);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }
}
