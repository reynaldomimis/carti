package com.upreyvan.carti.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.StringRes;

import java.util.HashMap;
import java.util.Map;


public class ToastHelper {

    public enum Status {
        SUCCESS, ERROR, INFO, WARNING
    }

    private static final long DEBOUNCE_INTERVAL = 2500;
    private static final long GLOBAL_THROTTLE = 1000;
    private static final Map<String, Long> lastShownMessages = new HashMap<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Toast currentToast;
    private static long lastGlobalTime = 0;

    public static void show(Context context, String message, Status status) {
        if (context == null || message == null || message.trim().isEmpty()) return;

        long now = System.currentTimeMillis();

        if (now - lastGlobalTime < GLOBAL_THROTTLE) {
            return;
        }

        synchronized (lastShownMessages) {
            if (lastShownMessages.containsKey(message)) {
                long lastTime = lastShownMessages.get(message);
                if (now - lastTime < DEBOUNCE_INTERVAL) {
                    return;
                }
            }
            lastShownMessages.put(message, now);
            lastGlobalTime = now;

            if (lastShownMessages.size() > 20) {
                lastShownMessages.entrySet().removeIf(entry -> now - entry.getValue() > DEBOUNCE_INTERVAL * 2);
            }
        }

        mainHandler.post(() -> {
            try {
                if (currentToast != null) {
                    currentToast.cancel();
                }
                // Use application context to avoid leaks
                currentToast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT);
                currentToast.show();
            } catch (Exception e) {
                // Fallback for edge cases
                android.util.Log.e("ToastHelper", "Error showing toast", e);
            }
        });
    }

    public static void show(Context context, @StringRes int messageRes, Status status) {
        if (context == null) return;
        show(context, context.getString(messageRes), status);
    }
}
