package com.upreyvan.carti.models;

import java.util.List;
import java.util.Map;

/**
 * Centralized data holder for all financial calculations.
 * Reflects the Phase 4 Summary Engine output.
 */
public record FinancialSummary(
    // Core Totals (Allocation-based for current system)
    double monthlyBalance,       // Current Month Allocation - Current Month Expenses
    double monthlyBudget,        // Sum of all ALLOCATIONS for current month
    double monthlyExpense,       // Sum of all EXPENSES for current month
    double todayExpense,         // Sum of all EXPENSES for today
    
    // Accumulated Savings
    double totalAccumulatedSavings,
    
    // Savings Progress (Still used by progress bars if needed)
    double savedAmount,          
    double targetAmount,         
    int savingsProgress,         
    
    // Future-ready Income (User-owned money)
    double monthlyIncome,        
    double monthlySavings,       
    
    // Trends
    double incomeTrend,          
    double expenseTrend,         
    double savingsTrend,         
    
    // Breakdowns
    List<CategoryTotal> categoryBreakdown,
    Map<String, Double> typeBreakdown,
    Map<String, Double> categoryBalances, // NEW: Remaining balance per category (Allocation - Spent)
    
    // Debts
    double totalDebt,
    
    // Meta
    long timestamp
) {
    public record CategoryTotal(
        String category,
        double amount,
        double percentage,
        int iconRes,
        int color
    ) {}
}
