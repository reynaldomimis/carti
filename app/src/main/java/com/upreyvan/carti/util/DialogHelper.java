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
                .setTitle(context.getString(com.upreyvan.carti.R.string.title_no_internet))
                .setMessage(context.getString(com.upreyvan.carti.R.string.msg_no_internet_transaction))
                .setCancelable(false)
                .setPositiveButton(context.getString(com.upreyvan.carti.R.string.btn_ok), null)
                .setIcon(com.upreyvan.carti.R.drawable.ic_chart)
                .show();
    }
}
