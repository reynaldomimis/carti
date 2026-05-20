package com.upreyvan.carti.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FormatUtils {
    public static String formatTime(Calendar calendar) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime());
    }
}
