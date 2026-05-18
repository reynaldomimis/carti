package com.upreyvan.carti.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "\\b(?:\\d[ -]*?){13,19}\\b|" + 
            "\\b\\d{10,12}\\b|" +          
            "password|secret|pin\\s\\d{4,6}", 
            Pattern.CASE_INSENSITIVE
    );

    public static boolean containsSensitiveInfo(String text) {
        if (text == null) return false;
        return SENSITIVE_PATTERN.matcher(text).find() ||
                text.toLowerCase().contains("bank account") ||
                text.toLowerCase().contains("credit card") ||
                text.toLowerCase().contains("cvv");
    }

    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
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
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(isoDate);
            if (date == null) return "";
            
            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return formatter.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                Date date = parser.parse(isoDate);
                return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
            } catch (Exception e2) {
                return "";
            }
        }
    }

    public static String formatTimestamp(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(isoDate);
            if (date == null) return "";

            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            return formatter.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                Date date = parser.parse(isoDate);
                return new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(date);
            } catch (Exception e2) {
                return "";
            }
        }
    }

    public static long getMillisFromIso(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return 0;
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(isoDate);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                Date date = parser.parse(isoDate);
                return date != null ? date.getTime() : 0;
            } catch (Exception e2) {
                return 0;
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
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "₱%,.2f", amount);
    }

    public static double getDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}