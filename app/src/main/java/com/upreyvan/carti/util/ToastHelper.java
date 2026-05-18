package com.upreyvan.carti.util;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class ToastHelper {

    public enum Status {
        SUCCESS, ERROR, INFO, WARNING
    }

    public static void show(Context context, String message, Status status) {
        if (context == null) return;
        // Simple implementation for now, can be customized with custom layouts
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void show(Context context, @StringRes int messageRes, Status status) {
        if (context == null) return;
        show(context, context.getString(messageRes), status);
    }
}
