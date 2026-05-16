package com.upreyvan.carti.base;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.upreyvan.carti.util.Constants;

public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    private VB binding;

    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        setupStatusBar();
        setupInsets();
        return binding.getRoot();
    }

    private void setupStatusBar() {
        if (getActivity() != null) {
            Window window = getActivity().getWindow();
            window.setStatusBarColor(Color.WHITE);
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(true);
        }
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            int top = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
    }

    protected VB getBinding() {
        return binding;
    }

    protected void showError(Throwable t) {
        String message = Constants.ErrorCodes.GENERIC_ERROR;
        if (t != null && t.getMessage() != null) {
            String msg = t.getMessage();
            // Map known error keys to user-friendly messages if needed, 
            // but for now, we just ensure we don't leak raw stack traces.
            if (msg.contains("UNAUTHORIZED") || msg.contains("401")) {
                message = "Session expired. Please login again.";
            } else if (msg.contains("NETWORK") || msg.contains("Unable to resolve host")) {
                message = "No internet connection.";
            }
        }
        
        if (getContext() != null) {
            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}