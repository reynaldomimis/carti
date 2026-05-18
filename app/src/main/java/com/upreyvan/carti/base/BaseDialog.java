package com.upreyvan.carti.base;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.upreyvan.carti.R;

public abstract class BaseDialog<VB extends ViewBinding> extends Dialog {

    protected VB binding;

    public BaseDialog(@NonNull Context context) {
        super(context, R.style.CustomDialogTheme);
    }

    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = inflateBinding(LayoutInflater.from(getContext()));
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}
