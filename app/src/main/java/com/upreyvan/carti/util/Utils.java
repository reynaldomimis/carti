package com.upreyvan.carti.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
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

    public static void showKeyboard(Context context, View view) {
        if (context != null && view != null) {
            view.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    public static void hideKeyboard(Context context, View view) {
        if (context != null && view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        }
        return false;
    }

    public static void setOptionalText(android.widget.TextView textView, String text) {
        if (text == null || text.trim().isEmpty()) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }

    public static String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "₱%,.2f", amount);
    }

    public static void showDateRangePicker(androidx.fragment.app.FragmentManager fragmentManager, com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener<androidx.core.util.Pair<Long, Long>> listener) {
        com.google.android.material.datepicker.MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Period")
                        .build();

        picker.addOnPositiveButtonClickListener(listener);
        picker.show(fragmentManager, "DATE_RANGE_PICKER");
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

    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public static String formatMonthYear(Calendar calendar) {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime());
    }

    public static String formatDateFull(Calendar calendar) {
        return new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(calendar.getTime());
    }

    public static String formatDateShort(Calendar calendar) {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.getTime());
    }

    public static String formatDateQuery(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
    }

    public static String formatTime(Calendar calendar) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime());
    }

    public static String formatMonthQuery(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
    }

    public static String formatDateRange(long start, long end) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        String startDate = sdf.format(new Date(start));
        String endDate = sdf.format(new Date(end));
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date(end));
        return String.format("%s - %s, %s", startDate, endDate, year);
    }

    public static long getMonthStartMillis(Calendar calendar) {
        Calendar start = (Calendar) calendar.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTimeInMillis();
    }

    public static long getMonthEndMillis(Calendar calendar) {
        Calendar end = (Calendar) calendar.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        return end.getTimeInMillis();
    }

    public static long getYearStartMillis(Calendar calendar) {
        Calendar start = (Calendar) calendar.clone();
        start.set(Calendar.MONTH, Calendar.JANUARY);
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTimeInMillis();
    }

    public static long getYearEndMillis(Calendar calendar) {
        Calendar end = (Calendar) calendar.clone();
        end.set(Calendar.MONTH, Calendar.DECEMBER);
        end.set(Calendar.DAY_OF_MONTH, 31);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        return end.getTimeInMillis();
    }

    public static String getMonthStart(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    public static String getMonthEnd(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    public static long getStartOfDayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static String joinStrings(java.util.List<?> list, String delimiter) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(String.valueOf(list.get(i)));
            if (i < list.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static int getCategoryColor(android.content.Context context, String category) {
        if (category == null) return context.getResources().getColor(com.upreyvan.carti.R.color.gray, null);
        
        switch (category.toLowerCase()) {
            case "food":
            case "food & drinks":
                return context.getResources().getColor(com.upreyvan.carti.R.color.icon_food, null);
            case "transport":
            case "transportation":
                return context.getResources().getColor(com.upreyvan.carti.R.color.icon_fare, null);
            case "bills":
            case "utilities":
                return context.getResources().getColor(com.upreyvan.carti.R.color.status_red, null);
            case "shopping":
                return context.getResources().getColor(com.upreyvan.carti.R.color.icon_store, null);
            case "health":
                return context.getResources().getColor(com.upreyvan.carti.R.color.icon_load, null);
            case "entertainment":
                return context.getResources().getColor(com.upreyvan.carti.R.color.purple, null);
            case "income":
            case "salary":
                return context.getResources().getColor(com.upreyvan.carti.R.color.carti_primary_green, null);
            default:
                return context.getResources().getColor(com.upreyvan.carti.R.color.gray, null);
        }
    }
}