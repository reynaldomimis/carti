package com.upreyvan.carti.utils;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.upreyvan.carti.R;
import com.upreyvan.carti.models.Transaction;
import java.util.Calendar;

public class GoalHelper {

    public static String getGoalStatusLabel(Context context, Transaction goal) {
        double current = goal.getAmount();
        double target = goal.getTargetAmount();
        String status = goal.getStatus();
        
        long targetMillis = DateHelper.getMillisFromIso(goal.getTargetDate());
        
        // Start of day comparison for overdue logic
        Calendar calNow = Calendar.getInstance();
        calNow.set(Calendar.HOUR_OF_DAY, 0);
        calNow.set(Calendar.MINUTE, 0);
        calNow.set(Calendar.SECOND, 0);
        calNow.set(Calendar.MILLISECOND, 0);
        long todayStart = calNow.getTimeInMillis();

        Calendar calTarget = Calendar.getInstance();
        if (targetMillis > 0) {
            calTarget.setTimeInMillis(targetMillis);
            calTarget.set(Calendar.HOUR_OF_DAY, 0);
            calTarget.set(Calendar.MINUTE, 0);
            calTarget.set(Calendar.SECOND, 0);
            calTarget.set(Calendar.MILLISECOND, 0);
        }
        long targetStart = calTarget.getTimeInMillis();

        boolean isOverdue = todayStart > targetStart && targetMillis > 0;
        boolean isReached = current >= target && target > 0;

        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "Goal Target Reached";
        }

        if (isReached) {
            return isOverdue ? "Completed After Deadline" : "Goal Target Reached";
        }

        if (isOverdue) {
            return "Overdue Target Date";
        }

        return "Target Date: " + DateHelper.formatDate(targetMillis);
    }

    public static int getLabelColor(Context context, Transaction goal) {
        double current = goal.getAmount();
        double target = goal.getTargetAmount();
        String status = goal.getStatus();
        
        long targetMillis = DateHelper.getMillisFromIso(goal.getTargetDate());
        
        Calendar calNow = Calendar.getInstance();
        calNow.set(Calendar.HOUR_OF_DAY, 0);
        calNow.set(Calendar.MINUTE, 0);
        calNow.set(Calendar.SECOND, 0);
        calNow.set(Calendar.MILLISECOND, 0);
        long todayStart = calNow.getTimeInMillis();

        Calendar calTarget = Calendar.getInstance();
        if (targetMillis > 0) {
            calTarget.setTimeInMillis(targetMillis);
            calTarget.set(Calendar.HOUR_OF_DAY, 0);
            calTarget.set(Calendar.MINUTE, 0);
            calTarget.set(Calendar.SECOND, 0);
            calTarget.set(Calendar.MILLISECOND, 0);
        }
        long targetStart = calTarget.getTimeInMillis();

        boolean isOverdue = todayStart > targetStart && targetMillis > 0;
        boolean isReached = current >= target && target > 0;

        if ("COMPLETED".equalsIgnoreCase(status)) {
            return ContextCompat.getColor(context, R.color.green_primary);
        }
        
        if ("CANCELED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            return ContextCompat.getColor(context, R.color.status_red);
        }

        // If overdue OR reached but not completed, and deadline passed, show red
        if (isOverdue) {
            return ContextCompat.getColor(context, R.color.status_red);
        }

        if (isReached) {
            // Reached on time or still within timeline
            return ContextCompat.getColor(context, R.color.status_green);
        }

        return ContextCompat.getColor(context, R.color.text_secondary);
    }
    
    public static boolean isTargetReached(Transaction goal) {
        return goal.getAmount() >= goal.getTargetAmount() && goal.getTargetAmount() > 0;
    }
}
