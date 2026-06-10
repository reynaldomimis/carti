package com.upreyvan.carti.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.upreyvan.carti.utils.Constants;
import com.upreyvan.carti.utils.SecurityManager;

import java.util.Calendar;

public class SalaryManager {
    private static SalaryManager instance;
    private final SharedPreferences prefs;

    private SalaryManager(Context context) {
        prefs = SecurityManager.getEncryptedPrefs(context, Constants.Keys.PREF_SALARY);
    }

    public static synchronized SalaryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SalaryManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setFirstPayday(int day) {
        prefs.edit().putInt(Constants.Keys.KEY_FIRST_PAYDAY, day).apply();
    }

    public int getFirstPayday() {
        return prefs.getInt(Constants.Keys.KEY_FIRST_PAYDAY, 15);
    }

    public void setSecondPayday(int day) {
        prefs.edit().putInt(Constants.Keys.KEY_SECOND_PAYDAY, day).apply();
    }

    public int getSecondPayday() {
        return prefs.getInt(Constants.Keys.KEY_SECOND_PAYDAY, 30);
    }

    public void setIsMonthly(boolean isMonthly) {
        prefs.edit().putBoolean(Constants.Keys.KEY_IS_MONTHLY, isMonthly).apply();
    }

    public boolean isMonthly() {
        return prefs.getBoolean(Constants.Keys.KEY_IS_MONTHLY, false);
    }

    public Calendar getNextPayday() {
        Calendar now = Calendar.getInstance();
        int currentDay = now.get(Calendar.DAY_OF_MONTH);

        int first = getFirstPayday();
        int second = getSecondPayday();
        boolean monthly = isMonthly();

        Calendar next = Calendar.getInstance();
        
        if (monthly) {
            if (currentDay < first) {
                int maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH);
                next.set(Calendar.DAY_OF_MONTH, Math.min(first, maxDays));
            } else {
                next.add(Calendar.MONTH, 1);
                int maxDays = next.getActualMaximum(Calendar.DAY_OF_MONTH);
                next.set(Calendar.DAY_OF_MONTH, Math.min(first, maxDays));
            }
        } else {
            if (currentDay < first) {
                next.set(Calendar.DAY_OF_MONTH, first);
            } else if (currentDay < second) {
                int maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH);
                next.set(Calendar.DAY_OF_MONTH, Math.min(second, maxDays));
            } else {
                next.add(Calendar.MONTH, 1);
                next.set(Calendar.DAY_OF_MONTH, first);
            }
        }
        
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        
        return next;
    }

    public Calendar getLastPayday() {
        Calendar next = getNextPayday();
        Calendar last = (Calendar) next.clone();
        
        if (isMonthly()) {
            last.add(Calendar.MONTH, -1);
        } else {
            // If next is 15th, last was 30th of prev month
            // If next is 30th, last was 15th of current month
            int first = getFirstPayday();
            if (next.get(Calendar.DAY_OF_MONTH) == first) {
                last.add(Calendar.MONTH, -1);
                int maxDays = last.getActualMaximum(Calendar.DAY_OF_MONTH);
                last.set(Calendar.DAY_OF_MONTH, Math.min(getSecondPayday(), maxDays));
            } else {
                last.set(Calendar.DAY_OF_MONTH, first);
            }
        }
        return last;
    }

    public int getDaysUntilNextPayday() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        Calendar next = getNextPayday();
        
        long diff = next.getTimeInMillis() - now.getTimeInMillis();
        int days = (int) (diff / (24 * 60 * 60 * 1000));
        return Math.max(0, days);
    }
    
    public int getTotalDaysInCycle() {
        Calendar last = getLastPayday();
        Calendar next = getNextPayday();
        long diff = next.getTimeInMillis() - last.getTimeInMillis();
        return (int) (diff / (24 * 60 * 60 * 1000));
    }

    public double getDailyBudget(double totalBudget) {
        int daysLeft = getDaysUntilNextPayday();
        if (daysLeft <= 0) return totalBudget;
        return totalBudget / daysLeft;
    }
}
