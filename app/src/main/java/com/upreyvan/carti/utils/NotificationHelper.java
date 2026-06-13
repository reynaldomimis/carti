package com.upreyvan.carti.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;

public class NotificationHelper {
    private static final String CHANNEL_ID = "carti_notifications";
    private static final String CHANNEL_NAME = "Carti Announcements";
    private static final String CHANNEL_DESC = "Notifications for bills and family updates";

    public static void showNotification(Context context, String title, String content, boolean isChat) {
        // 1. Check if notifications are globally enabled in app settings
        com.upreyvan.carti.managers.PreferenceManager pref = com.upreyvan.carti.managers.PreferenceManager.getInstance(context);
        if (!pref.isNotificationsEnabled()) {
            return;
        }

        // Granular logic is now handled in the Repository/Tracker 
        // to allow notifications even when app is open (e.g. while on Home screen)

        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (isChat) {
            intent.putExtra("navigate_to", "chat");
        } else {
            intent.putExtra("navigate_to", "notifications");
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ai_holder)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            int notifId = isChat ? 1001 : 1002;
            notificationManager.notify(notifId, builder.build());
            
            // Play sound for all notifications (Foreground & Background)
            playNotificationSound(context);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void showNotification(Context context, String title, String content) {
        showNotification(context, title, content, false);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, 
                    CHANNEL_NAME, 
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.enableVibration(true);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void playNotificationSound(Context context) {
        try {
            android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone r = android.media.RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
