package com.upreyvan.carti.util;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogHelper {

    public interface DialogCallback {
        void onConfirm();
    }

    public static void showConfirmation(Context context, String title, String message, String positiveButton, DialogCallback callback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(positiveButton, (dialog, which) -> callback.onConfirm())
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showNoInternetDialog(Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("No Internet Connection")
                .setMessage("You cannot add or update transaction. Please check your internet connection and try again.")
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .setIcon(com.upreyvan.carti.R.drawable.ic_chart)
                .show();
    }
}
