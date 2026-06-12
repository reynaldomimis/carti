package com.upreyvan.carti.utils;

import androidx.fragment.app.Fragment;
import com.upreyvan.carti.ui.family.FamilyChatFragment;
import com.upreyvan.carti.ui.notifications.NotificationsFragment;

public class AppLifecycleTracker {
    private static boolean isAppInForeground = false;
    private static Class<? extends Fragment> activeFragmentClass = null;

    public static void setAppInForeground(boolean inForeground) {
        isAppInForeground = inForeground;
    }

    public static boolean isAppInForeground() {
        return isAppInForeground;
    }

    public static void setActiveFragment(Fragment fragment) {
        if (fragment != null) {
            activeFragmentClass = fragment.getClass();
        } else {
            activeFragmentClass = null;
        }
    }

    public static boolean isChatActive() {
        return isAppInForeground && activeFragmentClass != null && 
               activeFragmentClass.equals(FamilyChatFragment.class);
    }

    public static boolean isNotificationsActive() {
        return isAppInForeground && activeFragmentClass != null && 
               activeFragmentClass.equals(NotificationsFragment.class);
    }
}
