package com.upreyvan.carti.base;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewbinding.ViewBinding;
import com.google.android.material.snackbar.Snackbar;
import com.upreyvan.carti.R;
import com.upreyvan.carti.util.LoadingDialog;
import com.upreyvan.carti.util.NetworkMonitor;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;


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
        
        applyEdgeToEdge();
        initNetworkMonitoring();
    }


    private void initNetworkMonitoring() {
        NetworkMonitor.getInstance(this).getStatus().observe(this, isOnline -> {
            if (!isOnline) {
                showNetworkSnackbar();
            } else if (networkSnackbar != null && networkSnackbar.isShown()) {
                networkSnackbar.dismiss();
            }
        });
    }

    private void showNetworkSnackbar() {
        if (networkSnackbar == null) {
            networkSnackbar = Snackbar.make(findViewById(android.R.id.content), 
                R.string.title_no_internet, 
                Snackbar.LENGTH_INDEFINITE);
            
            networkSnackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.status_red))
                    .setTextColor(Color.WHITE)
                    .setActionTextColor(Color.WHITE)
                    .setAction("CLOSE", v -> networkSnackbar.dismiss());
        }
        
        if (!networkSnackbar.isShown()) {
            networkSnackbar.show();
        }
    }

    protected boolean checkNetwork() {
        boolean online = NetworkMonitor.getInstance(this).isOnline();
        if (!online) showNetworkSnackbar();
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

    protected void setupDynamicPadding(View topView, View bottomView) {
        Utils.applySystemBarInsets(topView, bottomView, 1.0f, 0);
    }

    private void applyEdgeToEdge() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        loadingDialog = null;
        binding = null;
        super.onDestroy();
    }
}
