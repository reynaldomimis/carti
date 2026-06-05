package com.upreyvan.carti.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.util.NetworkMonitor;
import com.upreyvan.carti.util.UiHelper;

public abstract class BaseBottomSheetFragment<VB extends ViewBinding> extends BottomSheetDialogFragment {
    private VB binding;
    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @Override public void onCreate(@Nullable Bundle savedInstanceState) { super.onCreate(savedInstanceState); setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme); }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        return binding.getRoot();
    }

    protected VB getBinding() { return binding; }

    protected void showToast(String m, UiHelper.Status s) { UiHelper.showSnackbar(getView(), m, s); }

    protected void showToast(int res, UiHelper.Status s) { UiHelper.showSnackbar(getView(), res, s); }

    protected void showLoading(boolean l) { if (getActivity() instanceof BaseActivity) ((BaseActivity<?>) getActivity()).showLoading(l); }

    protected void showLoading(boolean l, String m) { if (getActivity() instanceof BaseActivity) ((BaseActivity<?>) getActivity()).showLoading(l, m); }

    protected boolean checkNetwork() { return NetworkMonitor.getInstance(requireContext()).isOnline(); }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
