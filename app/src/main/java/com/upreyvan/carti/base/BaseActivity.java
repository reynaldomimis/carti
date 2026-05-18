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

import com.upreyvan.carti.util.DialogHelper;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;

public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    private VB binding;

    protected abstract VB inflateBinding(LayoutInflater inflater);

    protected void showToast(String message, ToastHelper.Status status) {
        ToastHelper.show(this, message, status);
    }

    protected void showToast(int resId, ToastHelper.Status status) {
        ToastHelper.show(this, resId, status);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());
        setupSystemUI();
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
