package com.upreyvan.carti.base;

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
import com.upreyvan.carti.R;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.NetworkMonitor;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Utils;

public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {
    private VB binding;
    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        setupStatusBar();
        setupInsets();
        return binding.getRoot();
    }

    private void setupStatusBar() {
        // Handled by BaseActivity for true edge-to-edge
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int t = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), t, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    protected void navigateTo(Fragment f) {
        if (getActivity() != null && isAdded()) {
            int id = getId() > 0 ? getId() : (getActivity().findViewById(R.id.start_fragment_container) != null ? R.id.start_fragment_container : R.id.fragment_container);
            getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out).replace(id, f).addToBackStack(null).commitAllowingStateLoss();
        }
    }

    protected void setupToolbar(com.upreyvan.carti.databinding.LayoutCustomToolbarBinding b, String t) {
        b.tvToolbarTitle.setText(t);
        b.btnBack.setOnClickListener(v -> { if (getActivity() != null) getActivity().onBackPressed(); });
    }

    protected void setupToolbar(com.upreyvan.carti.databinding.LayoutCustomToolbarBinding b, int res) { setupToolbar(b, getString(res)); }

    protected void setupDynamicPadding(View h, View s, float r) { Utils.applySystemBarInsets(h, s, r, getResources().getDimensionPixelSize(R.dimen.bottom_nav_medium)); }

    protected VB getBinding() { return binding; }

    protected void showToast(String m, ToastHelper.Status s) { ToastHelper.show(getContext(), m, s); }

    protected void showToast(int res, ToastHelper.Status s) { ToastHelper.show(getContext(), res, s); }

    protected void showLoading(boolean l) { if (getActivity() instanceof BaseActivity) ((BaseActivity<?>) getActivity()).showLoading(l); }

    protected void showLoading(boolean l, String m) { if (getActivity() instanceof BaseActivity) ((BaseActivity<?>) getActivity()).showLoading(l, m); }

    protected boolean checkNetwork() { return NetworkMonitor.getInstance(requireContext()).isOnline(); }

    protected void showError(Throwable t) {
        String m = Constants.ErrorCodes.GENERIC_ERROR;
        if (t != null && t.getMessage() != null) {
            String msg = t.getMessage();
            if (msg.contains("401")) m = "Session expired. Please login again.";
            else if (msg.contains("NETWORK")) m = "No internet connection.";
        }
        showToast(m, ToastHelper.Status.ERROR);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
