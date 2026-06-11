package com.upreyvan.carti.managers.ai;

import android.content.Context;
import com.upreyvan.carti.models.Bill;
import com.upreyvan.carti.models.FinancialSummary;
import com.upreyvan.carti.repository.AiRepository;
import java.util.List;

/**
 * Phase 4: Centralized AI Insight Engine.
 * Integrates with FinancialSummary to provide prioritized awareness insights.
 */
public class AiInsightEngine {

    private final AiRepository aiRepo;

    public AiInsightEngine(Context context) {
        this.aiRepo = AiRepository.getInstance(context);
    }

    public void generateInsights(FinancialSummary summary, List<Bill> bills, AiRepository.AiCallback callback) {
        if (summary == null) return;

        // 1. Build prioritized context from Centralized Summary
        StringBuilder contextBuilder = new StringBuilder();
        
        // Priority 1: Bills (Urgency)
        if (bills != null && !bills.isEmpty()) {
            contextBuilder.append("URGENT BILLS:\n");
            for (Bill b : bills) {
                contextBuilder.append(String.format("- %s (₱%.2f) Due: %s Status: %s\n", 
                    b.getName(), b.getAmount(), b.getDate(), b.getStatus()));
            }
        }

        // Priority 2: Budget & Balance (Allocation-based)
        contextBuilder.append("FINANCIAL STATUS (ALLOCATION-BASED):\n");
        contextBuilder.append(String.format("- Current Monthly Balance: ₱%.2f\n", summary.monthlyBalance()));
        contextBuilder.append(String.format("- Total Accumulated Savings: ₱%.2f\n", summary.totalAccumulatedSavings()));
        contextBuilder.append(String.format("- Monthly Budget: ₱%.2f\n", summary.monthlyBudget()));
        contextBuilder.append(String.format("- Spent this Month: ₱%.2f\n", summary.monthlyExpense()));
        contextBuilder.append(String.format("- Spent Today: ₱%.2f\n", summary.todayExpense()));
        
        // Priority 3: Trends & Awareness
        contextBuilder.append("ANALYTICS & TRENDS:\n");
        contextBuilder.append(String.format("- Expense Trend: %.1f%% vs last month\n", summary.expenseTrend()));
        if (summary.monthlyIncome() > 0) {
            contextBuilder.append(String.format("- Monthly Income (Future-Ready): ₱%.2f\n", summary.monthlyIncome()));
            contextBuilder.append(String.format("- Estimated Savings: ₱%.2f\n", summary.monthlySavings()));
        }

        // Priority 4: Category Spikes
        if (summary.categoryBreakdown() != null && !summary.categoryBreakdown().isEmpty()) {
            contextBuilder.append("TOP CATEGORIES:\n");
            summary.categoryBreakdown().stream()
                .sorted((a, b) -> Double.compare(b.amount(), a.amount()))
                .limit(3)
                .forEach(c -> contextBuilder.append(String.format("- %s: ₱%.2f (%.0f%% of total)\n", 
                    c.category(), c.amount(), c.percentage())));
        }

        // 2. Delegate to AI Manager with prioritized context
        aiRepo.getInsights(contextBuilder.toString(), callback);
    }
}
