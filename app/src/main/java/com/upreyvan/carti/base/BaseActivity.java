package com.upreyvan.carti.base;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewbinding.ViewBinding;
import com.google.android.material.snackbar.Snackbar;
import com.upreyvan.carti.R;
import com.upreyvan.carti.util.LoadingDialog;
import com.upreyvan.carti.util.NetworkMonitor;
import com.upreyvan.carti.util.ToastHelper;

public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {
    private VB binding;
    private LoadingDialog loadingDialog;
    private Snackbar networkSnackbar;

    protected abstract VB inflateBinding(LayoutInflater inflater);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());
        setupSystemUI();
        initNetworkMonitoring();
    }

    private void initNetworkMonitoring() {
        NetworkMonitor.getInstance(this).getStatus().observe(this, isOnline -> {
            if (!isOnline) {
                showNetworkError();
            } else if (networkSnackbar != null) {
                networkSnackbar.dismiss();
            }
        });
    }

    private void showNetworkError() {
        if (networkSnackbar == null) {
            networkSnackbar = Snackbar.make(findViewById(android.R.id.content), 
                "No Internet Connection. Please check your network.",
                Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(Color.RED)
                .setTextColor(Color.WHITE);
        }
        if (!networkSnackbar.isShown()) networkSnackbar.show();
    }

    protected boolean checkNetwork() {
        boolean online = NetworkMonitor.getInstance(this).isOnline();
        if (!online) showNetworkError();
        return online;
    }

    protected void showToast(String message, ToastHelper.Status status) {
        ToastHelper.show(this, message, status);
    }

    protected void showToast(int resId, ToastHelper.Status status) {
        ToastHelper.show(this, resId, status);
    }

    protected void showLoading(boolean isLoading) {
        showLoading(isLoading, null);
    }

    protected void showLoading(boolean isLoading, @Nullable String message) {
        if (isFinishing()) return;
        if (isLoading) {
            if (loadingDialog == null) loadingDialog = new LoadingDialog(this);
            if (message != null) loadingDialog.setMessage(message);
            if (!loadingDialog.isShowing()) loadingDialog.show();
        } else if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    protected VB getBinding() { return binding; }

    private void setupSystemUI() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }
}
