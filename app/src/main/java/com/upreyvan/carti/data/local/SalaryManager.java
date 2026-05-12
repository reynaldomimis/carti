package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public class SalaryManager {
    private static final String PREF_NAME = "salary_prefs";
    private static final String KEY_SALARY_AMOUNT = "salary_amount";
    private static final String KEY_FIRST_PAYDAY = "first_payday";
    private static final String KEY_SECOND_PAYDAY = "second_payday";

    private static SalaryManager instance;
    private final SharedPreferences prefs;

    private SalaryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SalaryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SalaryManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setSalaryAmount(float amount) {
        prefs.edit().putFloat(KEY_SALARY_AMOUNT, amount).apply();
    }

    public float getSalaryAmount() {
        return prefs.getFloat(KEY_SALARY_AMOUNT, 0f);
    }

    public void setFirstPayday(int day) {
        prefs.edit().putInt(KEY_FIRST_PAYDAY, day).apply();
    }

    public int getFirstPayday() {
        return prefs.getInt(KEY_FIRST_PAYDAY, 15); // Default 15th
    }

    public void setSecondPayday(int day) {
        prefs.edit().putInt(KEY_SECOND_PAYDAY, day).apply();
    }

    public int getSecondPayday() {
        return prefs.getInt(KEY_SECOND_PAYDAY, 30); // Default 30th
    }

    public Calendar getNextPayday() {
        Calendar now = Calendar.getInstance();
        int currentDay = now.get(Calendar.DAY_OF_MONTH);

        int first = getFirstPayday();
        int second = getSecondPayday();

        Calendar next = Calendar.getInstance();
        if (currentDay < first) {
            next.set(Calendar.DAY_OF_MONTH, first);
        } else if (currentDay < second) {
            // Check if month has enough days for 'second'
            int maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH);
            next.set(Calendar.DAY_OF_MONTH, Math.min(second, maxDays));
        } else {
            next.add(Calendar.MONTH, 1);
            next.set(Calendar.DAY_OF_MONTH, first);
        }
        
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        
        return next;
    }

    public int getDaysUntilNextPayday() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        Calendar next = getNextPayday();
        
        long diff = next.getTimeInMillis() - now.getTimeInMillis();
        return (int) (diff / (24 * 60 * 60 * 1000));
    }
    
    public double getDailyBudget() {
        int days = getDaysUntilNextPayday();
        if (days <= 0) return getSalaryAmount(); // Payday today or error
        return getSalaryAmount() / days;
    }
}
