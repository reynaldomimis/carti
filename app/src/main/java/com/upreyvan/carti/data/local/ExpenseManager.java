package com.upreyvan.carti.data.local;

import com.upreyvan.carti.model.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpenseManager {
    private static ExpenseManager instance;
    private final List<Transaction> transactions;
    private OnExpenseChangeListener listener;

    public interface OnExpenseChangeListener {
        void onExpensesUpdated();
    }

    private ExpenseManager() {
        transactions = new ArrayList<>();
    }

    public static synchronized ExpenseManager getInstance() {
        if (instance == null) {
            instance = new ExpenseManager();
        }
        return instance;
    }

    public void setOnExpenseChangeListener(OnExpenseChangeListener listener) {
        this.listener = listener;
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(0, transaction);
        if (listener != null) {
            listener.onExpensesUpdated();
        }
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }
    
    public List<Transaction> getRecentTransactions(int limit) {
        int size = transactions.size();
        return new ArrayList<>(transactions.subList(0, Math.min(size, limit)));
    }

    public double getTodayTotalSpent() {
        double total = 0;
        Calendar today = Calendar.getInstance();
        int day = today.get(Calendar.DAY_OF_YEAR);
        int year = today.get(Calendar.YEAR);

        for (Transaction t : transactions) {
            Calendar tDate = Calendar.getInstance();
            tDate.setTimeInMillis(t.getTimestampMillis());
            if (tDate.get(Calendar.DAY_OF_YEAR) == day && tDate.get(Calendar.YEAR) == year) {
                try {
                    String cleanAmount = t.getAmount().replace("₱", "").replace(",", "").trim();
                    total += Double.parseDouble(cleanAmount);
                } catch (Exception ignored) {}
            }
        }
        return total;
    }
}
