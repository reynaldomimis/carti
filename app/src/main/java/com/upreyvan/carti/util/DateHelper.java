package com.upreyvan.carti.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateHelper {

    private static final String ISO_FORMAT_EXTENDED = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final String ISO_FORMAT_SHORT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public static String formatIsoToTime(String isoDate) {
        Date date = parseIso(isoDate);
        if (date == null) return "";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    public static String formatIsoToFull(String isoDate) {
        Date date = parseIso(isoDate);
        if (date == null) return "";
        return new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(date);
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(millis));
    }

    public static long getMillisFromIso(String isoDate) {
        Date date = parseIso(isoDate);
        return date != null ? date.getTime() : 0;
    }

    private static Date parseIso(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            SimpleDateFormat parser = new SimpleDateFormat(ISO_FORMAT_EXTENDED, Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            return parser.parse(isoDate);
        } catch (Exception e) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(ISO_FORMAT_SHORT, Locale.US);
                return parser.parse(isoDate);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT_EXTENDED, Locale.US);
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

    public static String formatMonthQuery(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
    }

    public static String formatTime(Calendar calendar) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime());
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
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT_EXTENDED, Locale.US);
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
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT_EXTENDED, Locale.US);
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
}
