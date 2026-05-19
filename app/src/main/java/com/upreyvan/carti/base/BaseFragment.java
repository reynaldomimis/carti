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
import com.upreyvan.carti.util.DialogHelper;
import com.upreyvan.carti.util.Utils;

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

    protected void navigateTo(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(com.upreyvan.carti.R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    protected void setupToolbar(com.upreyvan.carti.databinding.LayoutCustomToolbarBinding toolbarBinding, String title) {
        toolbarBinding.tvToolbarTitle.setText(title);
        toolbarBinding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    protected void setupToolbar(com.upreyvan.carti.databinding.LayoutCustomToolbarBinding toolbarBinding, int titleRes) {
        setupToolbar(toolbarBinding, getString(titleRes));
    }

    protected void setupDynamicPadding(View header, View scrollable, float ratio) {
        com.upreyvan.carti.util.Utils.applySystemBarInsets(
                header,
                scrollable,
                ratio,
                getResources().getDimensionPixelSize(com.upreyvan.carti.R.dimen.bottom_nav_medium)
        );
    }

    protected VB getBinding() {
        return binding;
    }

    protected void showToast(String message, com.upreyvan.carti.util.ToastHelper.Status status) {
        com.upreyvan.carti.util.ToastHelper.show(getContext(), message, status);
    }

    protected void showToast(int resId, com.upreyvan.carti.util.ToastHelper.Status status) {
        com.upreyvan.carti.util.ToastHelper.show(getContext(), resId, status);
    }

    protected void showLoading(boolean isLoading) {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity<?>) getActivity()).showLoading(isLoading);
        }
    }

    protected void showLoading(boolean isLoading, String message) {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity<?>) getActivity()).showLoading(isLoading, message);
        }
    }

    protected boolean checkNetwork() {
        if (!Utils.isNetworkAvailable(requireContext())) {
            DialogHelper.showNoInternetDialog(requireContext());
            return false;
        }
        return true;
    }

    protected void executeWithNetwork(Runnable action) {
        if (checkNetwork()) {
            action.run();
        }
    }

    protected void showError(Throwable t) {
        String message = Constants.ErrorCodes.GENERIC_ERROR;
        if (t != null && t.getMessage() != null) {
            String msg = t.getMessage();
            if (msg.contains("UNAUTHORIZED") || msg.contains("401")) {
                message = "Session expired. Please login again.";
            } else if (msg.contains("NETWORK") || msg.contains("Unable to resolve host")) {
                message = "No internet connection.";
            }
        }
        
        showToast(message, com.upreyvan.carti.util.ToastHelper.Status.ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}