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
import com.upreyvan.carti.util.DialogHelper;
import com.upreyvan.carti.util.Utils;

public abstract class BaseBottomSheetFragment<VB extends ViewBinding> extends BottomSheetDialogFragment {

    private VB binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        return binding.getRoot();
    }

    protected VB getBinding() {
        return binding;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
