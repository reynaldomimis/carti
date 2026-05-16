package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.util.Constants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpenseManager {
    private static ExpenseManager instance;
    private final List<Transaction> transactions;
    private final SharedPreferences prefs;
    private final Gson gson;
    private OnExpenseChangeListener listener;

    public interface OnExpenseChangeListener {
        void onExpensesUpdated();
    }

    private ExpenseManager(Context context) {
        prefs = context.getSharedPreferences(Constants.Keys.PREF_EXPENSE, Context.MODE_PRIVATE);
        gson = new Gson();
        transactions = loadTransactions();
    }

    public static synchronized ExpenseManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("ExpenseManager must be initialized with Context first");
        }
        return instance;
    }

    public static synchronized ExpenseManager init(Context context) {
        if (instance == null) {
            instance = new ExpenseManager(context.getApplicationContext());
        }
        return instance;
    }

    private List<Transaction> loadTransactions() {
        String json = prefs.getString(Constants.Keys.KEY_TRANSACTIONS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<Transaction>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveTransactions() {
        prefs.edit().putString(Constants.Keys.KEY_TRANSACTIONS, gson.toJson(transactions)).apply();
    }

    public void setOnExpenseChangeListener(OnExpenseChangeListener listener) {
        this.listener = listener;
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(0, transaction);
        saveTransactions();
        if (listener != null) {
            listener.onExpensesUpdated();
        }
    }

    public void setTransactions(List<Transaction> newTransactions) {
        transactions.clear();
        transactions.addAll(newTransactions);
        // Sort by timestamp descending
        transactions.sort((t1, t2) -> Long.compare(t2.getTimestampMillis(), t1.getTimestampMillis()));
        saveTransactions();
        if (listener != null) {
            listener.onExpensesUpdated();
        }
    }

    public void clear() {
        transactions.clear();
        saveTransactions();
        if (listener != null) {
            listener.onExpensesUpdated();
        }
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }
    
    public List<Transaction> getRecentTransactions(int limit) {
        // Sort by timestamp descending
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort((t1, t2) -> Long.compare(t2.getTimestampMillis(), t1.getTimestampMillis()));
        int size = sorted.size();
        return new ArrayList<>(sorted.subList(0, Math.min(size, limit)));
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
