package com.upreyvan.carti.util;

import java.util.Locale;

public class StringHelper {

    public static String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "₱%,.2f", amount);
    }

    public static String getStringValue(Object value) {
        if (value == null) return "";
        return String.valueOf(value);
    }

    public static double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
