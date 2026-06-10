package com.upreyvan.carti.utils;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.StringRes;

@Deprecated
public class ToastHelper {

    public enum Status {
        SUCCESS, ERROR, INFO, WARNING
    }

    public static void show(Context context, String message, Status status) {
        show(context, message, mapStatus(status));
    }

    public static void show(Context context, String message, UiHelper.Status status) {
        if (context instanceof Activity) {
            UiHelper.showSnackbar((Activity) context, message, status);
        } else {
            android.util.Log.i("ToastHelper", "Redirected Toast to Snackbar: " + message);
        }
    }

    public static void show(Context context, @StringRes int messageRes, Status status) {
        show(context, messageRes, mapStatus(status));
    }

    public static void show(Context context, @StringRes int messageRes, UiHelper.Status status) {
        if (context == null) return;
        show(context, context.getString(messageRes), status);
    }

    private static UiHelper.Status mapStatus(Status status) {
        return switch (status) {
            case SUCCESS -> UiHelper.Status.SUCCESS;
            case ERROR -> UiHelper.Status.ERROR;
            case WARNING -> UiHelper.Status.WARNING;
            default -> UiHelper.Status.INFO;
        };
    }
}
