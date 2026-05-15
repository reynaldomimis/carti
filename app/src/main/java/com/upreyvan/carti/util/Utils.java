package com.upreyvan.carti.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return "Just now";
        }

        final long diff = now - time;
        if (diff < 60 * 1000) {
            return "Just now";
        } else if (diff < 2 * 60 * 1000) {
            return "1 min ago";
        } else if (diff < 50 * 60 * 1000) {
            return diff / (60 * 1000) + " mins ago";
        } else if (diff < 90 * 60 * 1000) {
            return "1 hour ago";
        } else if (diff < 24 * 60 * 60 * 1000) {
            return diff / (60 * 60 * 1000) + " hours ago";
        } else if (diff < 48 * 60 * 60 * 1000) {
            return "yesterday";
        } else {
            return diff / (24 * 60 * 60 * 1000) + " days ago";
        }
    }

    public static String formatIsoDateToTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            // Appwrite uses ISO 8601: 2023-05-14T09:45:33.619+00:00 or 2023-05-14T09:45:33Z
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(isoDate);
            if (date == null) return "";
            
            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return formatter.format(date);
        } catch (Exception e) {
            // Fallback for different ISO variations
            try {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                Date date = parser.parse(isoDate);
                return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
            } catch (Exception e2) {
                return "";
            }
        }
    }

    public static String getGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay < 12) {
            return "Good morning";
        } else if (timeOfDay < 17) {
            return "Good afternoon";
        } else {
            return "Good evening";
        }
    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static void applySystemBarInsets(
            View topView,
            View bottomView,
            float topMultiplier,
            int customBarHeight
    ) {

        int originalTopPadding = topView.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(topView, (v, insets) -> {

            Insets systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int statusBarHeight = systemBars.top;

            int adjustedTop =
                    (int) (statusBarHeight * topMultiplier);

            v.setPadding(
                    v.getPaddingLeft(),
                    originalTopPadding + adjustedTop,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );

            return insets;
        });

        if (bottomView != null) {
            final int originalBottomPadding = bottomView.getPaddingBottom();
            final ViewGroup.LayoutParams layoutParams = bottomView.getLayoutParams();
            final int originalBottomMargin = (layoutParams instanceof ViewGroup.MarginLayoutParams) ?
                    ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin : 0;

            ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                int systemNavHeight = systemBars.bottom;

                if (v instanceof androidx.core.widget.NestedScrollView || v instanceof androidx.recyclerview.widget.RecyclerView) {
                    v.setPadding(
                            v.getPaddingLeft(),
                            v.getPaddingTop(),
                            v.getPaddingRight(),
                            originalBottomPadding + systemNavHeight + customBarHeight
                    );
                } else {
                    if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        lp.bottomMargin = originalBottomMargin + systemNavHeight + customBarHeight;
                        v.setLayoutParams(lp);
                    }
                }

                return insets;
            });
        }
    }

    public static void showToast(Context context, String message) {
        if (context != null && message != null) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}