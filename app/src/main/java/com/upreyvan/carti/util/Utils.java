package com.upreyvan.carti.util;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.upreyvan.carti.R;
import com.upreyvan.carti.model.Transaction;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
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
        if (time < 1000000000000L) time *= 1000;
        long now = System.currentTimeMillis();
        if (time > now || time <= 0) return "Just now";

        long diff = now - time;
        if (diff < 60000) return "Just now";
        if (diff < 120000) return "1 min ago";
        if (diff < 3000000) return diff / 60000 + " mins ago";
        if (diff < 5400000) return "1 hour ago";
        if (diff < 86400000) return diff / 3600000 + " hours ago";
        if (diff < 172800000) return "yesterday";
        return diff / 86400000 + " days ago";
    }

    public static String formatIsoDateToTime(String iso) { return DateHelper.formatIsoToTime(iso); }
    public static String formatTimestamp(String iso) { return DateHelper.formatIsoToFull(iso); }
    public static long getMillisFromIso(String iso) { return DateHelper.getMillisFromIso(iso); }
    public static String getCurrentTimestamp() { return DateHelper.getCurrentTimestamp(); }
    public static String formatMonthYear(Calendar cal) { return DateHelper.formatMonthYear(cal); }
    public static String formatDateFull(Calendar cal) { return DateHelper.formatDateFull(cal); }
    public static String formatDateShort(Calendar cal) { return DateHelper.formatDateShort(cal); }
    public static String formatDateQuery(Calendar cal) { return DateHelper.formatDateQuery(cal); }
    public static String formatMonthQuery(Calendar cal) { return DateHelper.formatMonthQuery(cal); }
    public static String formatTime(Calendar cal) { return DateHelper.formatTime(cal); }
    public static String formatDate(long millis) { return DateHelper.formatDate(millis); }
    public static String formatDateRange(long start, long end) { return DateHelper.formatDateRange(start, end); }
    public static long getMonthStartMillis(Calendar cal) { return DateHelper.getMonthStartMillis(cal); }
    public static long getMonthEndMillis(Calendar cal) { return DateHelper.getMonthEndMillis(cal); }
    public static long getYearStartMillis(Calendar cal) { return DateHelper.getYearStartMillis(cal); }
    public static long getYearEndMillis(Calendar cal) { return DateHelper.getYearEndMillis(cal); }
    public static String getMonthStart(int m, int y) { return DateHelper.getMonthStart(m, y); }
    public static String getMonthEnd(int m, int y) { return DateHelper.getMonthEnd(m, y); }
    public static long getStartOfDayMillis() { return DateHelper.getStartOfDayMillis(); }

    public static String formatCurrency(double amount) { return CurrencyHelper.format(amount); }
    public static String formatCompactCurrency(double amount) { return CurrencyHelper.formatCompact(amount); }
    public static double getDouble(Object val) { return CurrencyHelper.parse(val); }

    public static boolean isNetworkAvailable(Context ctx) { return NetworkHelper.isNetworkAvailable(ctx); }

    public static void showToast(Context context, String message) {
        ToastHelper.show(context, message, UiHelper.Status.INFO);
    }

    public static void applySystemBarInsets(View t, View b, float m, int h) { UiHelper.applySystemBarInsets(t, b, m, h); }
    public static void showKeyboard(Context ctx, View v) { UiHelper.showKeyboard(ctx, v); }
    public static void hideKeyboard(Context ctx, View v) { UiHelper.hideKeyboard(ctx, v); }
    public static void setOptionalText(TextView tv, String txt) { UiHelper.setOptionalText(tv, txt); }

    public static String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    public static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static void showDateRangePicker(FragmentManager fm, MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>> l) {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Period")
                .build();
        picker.addOnPositiveButtonClickListener(l);
        picker.show(fm, "DATE_RANGE_PICKER");
    }

    public static String joinStrings(List<?> list, String delimiter) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(delimiter);
        }
        return sb.toString();
    }

    public static Transaction parseTransaction(Map<String, Object> d, String id, String c, String u) {
        return TransactionHelper.parse(d, id, c, u);
    }

    public static int getCategoryColor(Context ctx, String category) {
        if (category == null) return ctx.getColor(R.color.gray);
        int resId = switch (category.toLowerCase()) {
            case "food", "food & drinks" -> R.color.icon_food;
            case "transport", "transportation" -> R.color.icon_fare;
            case "bills", "utilities" -> R.color.status_red;
            case "shopping" -> R.color.icon_store;
            case "health" -> R.color.icon_load;
            case "entertainment" -> R.color.purple;
            case "income", "salary" -> R.color.carti_primary_green;
            default -> R.color.gray;
        };
        return ctx.getColor(resId);
    }
}


