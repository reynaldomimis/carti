package com.upreyvan.carti.util;

import java.util.Locale;

public class CurrencyHelper {

    public static String format(double amount) {
        return String.format(Locale.getDefault(), "₱%,.2f", amount);
    }

    public static String formatCompact(double amount) {
        return String.format(Locale.getDefault(), "₱%,.0f", amount);
    }

    public static double parse(Object value) {
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
