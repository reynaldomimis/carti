package com.upreyvan.carti.managers;

import com.upreyvan.carti.models.FinancialSummary;
import com.upreyvan.carti.models.Transaction;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 4 Centralized Financial Calculation Engine.
 */
public class FinancialEngine {

    public static FinancialSummary calculate(List<TransactionWithUser> transactions) {
        if (transactions == null) {
            return new FinancialSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new ArrayList<>(), new HashMap<>(), new HashMap<>(), 0, System.currentTimeMillis());
        }

        // Time Contexts
        Calendar cal = Calendar.getInstance();
        long curMonthStart = Utils.getMonthStartMillis(cal);
        long curMonthEnd = Utils.getMonthEndMillis(cal);
        long todayStart = Utils.getStartOfDayMillis();
        String curMonthKey = Utils.formatMonthQuery(cal);

        cal.add(Calendar.MONTH, -1);
        long lastMonthStart = Utils.getMonthStartMillis(cal);
        long lastMonthEnd = Utils.getMonthEndMillis(cal);

        // Monthly Calculation Buckets
        double budget = 0, expense = 0, income = 0, today = 0;
        double lastExpense = 0, lastIncome = 0;
        
        // All-time Calculation Buckets (For Accumulated Savings)
        double allTimeAllocation = 0;
        double allTimeExpense = 0;
        double allTimeGoalSaved = 0;
        
        double totalDebt = 0;
        Map<String, Double> typeTotals = new HashMap<>();
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Double> categoryAllocations = new HashMap<>();
        Map<String, Double> categorySpent = new HashMap<>();

        for (TransactionWithUser tu : transactions) {
            Transaction t = tu.getTransaction();
            String type = t.getType() != null ? t.getType().toUpperCase() : "EXPENSE";
            long ts = t.getTimestampMillis();
            double amt = t.getAmount();

            typeTotals.put(type, typeTotals.getOrDefault(type, 0.0) + amt);

            boolean isCurMonth = ts >= curMonthStart && ts <= curMonthEnd;
            boolean isLastMonth = ts >= lastMonthStart && ts <= lastMonthEnd;

            switch (type) {
                case "ALLOCATION" -> {
                    allTimeAllocation += amt;
                    if (t.getAllocationMonth() != null && t.getAllocationMonth().contains(curMonthKey)) {
                        budget += amt;
                        categoryAllocations.put(t.getCategory(), categoryAllocations.getOrDefault(t.getCategory(), 0.0) + amt);
                    }
                }
                case "EXPENSE" -> {
                    allTimeExpense += amt;
                    if (isCurMonth) {
                        expense += amt;
                        categoryTotals.put(t.getCategory(), categoryTotals.getOrDefault(t.getCategory(), 0.0) + amt);
                        
                        // Track spent per main category for balance calculation
                        categorySpent.put(t.getCategory(), categorySpent.getOrDefault(t.getCategory(), 0.0) + amt);
                    }
                    if (isLastMonth) lastExpense += amt;
                    if (ts >= todayStart) today += amt;
                }
                case "INCOME" -> {
                    if (isCurMonth) income += amt;
                    if (isLastMonth) lastIncome += amt;
                }
                case "DEBT" -> {
                    if (!t.isPaid()) totalDebt += amt;
                }
                case "GOAL", "GOAL_FUNDS" -> {
                    allTimeGoalSaved += amt;
                }
            }
        }

        // 1. Monthly Balance (Available for spending now)
        double monthlyBalance = budget - expense;
        
        // 2. Total Accumulated Savings (Carry-over logic: Option A)
        double totalAccumulatedSavings = (allTimeAllocation - allTimeExpense) + allTimeGoalSaved;
        
        // Trends & Savings Logic (Future-ready)
        double monthlySavingsIncome = income - expense;
        double incomeTrend = calculateTrend(income, lastIncome);
        double expenseTrend = calculateTrend(expense, lastExpense);
        double savingsTrend = calculateTrend(monthlyBalance, (budget - lastExpense));

        // 3. Category Breakdown for Chart (STRICTLY type = EXPENSE)
        List<FinancialSummary.CategoryTotal> breakdown = new ArrayList<>();
        double totalExpenseForChart = expense;
        categoryTotals.forEach((cat, sum) -> {
            double percent = totalExpenseForChart > 0 ? (sum / totalExpenseForChart) * 100 : 0;
            breakdown.add(new FinancialSummary.CategoryTotal(cat, sum, percent, 0, 0));
        });
        
        Utils.sortAlphabetically(breakdown, FinancialSummary.CategoryTotal::category);

        // 4. Category Balances for Budget Info
        Map<String, Double> categoryBalances = new HashMap<>();
        categoryAllocations.forEach((cat, alloc) -> {
            double spent = categorySpent.getOrDefault(cat, 0.0);
            categoryBalances.put(cat, alloc - spent);
        });

        return new FinancialSummary(
            monthlyBalance, budget, expense, today,
            totalAccumulatedSavings,
            totalAccumulatedSavings, // savedAmount
            budget,                  // targetAmount
            (int)(budget > 0 ? (monthlyBalance/budget*100) : 0), // savingsProgress
            income, monthlySavingsIncome,
            incomeTrend, expenseTrend, savingsTrend,
            breakdown, typeTotals, categoryBalances, totalDebt,
            System.currentTimeMillis()
        );
    }

    private static double calculateTrend(double c, double p) { return p == 0 ? 0 : ((c - p) / p) * 100; }
}
