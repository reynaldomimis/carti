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
                .setPositiveButton(positiveButton, (dialog, which) -> callback.onConfirm())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
