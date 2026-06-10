package com.upreyvan.carti.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.upreyvan.carti.R;

public class LoadingDialog extends Dialog {

    private final TextView tvMessage;

    public LoadingDialog(@NonNull Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_loading);
        
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        setCancelable(false);
        tvMessage = findViewById(R.id.tvMessage);
    }

    public void setMessage(String message) {
        tvMessage.setText(message);
    }
}
