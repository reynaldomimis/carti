package com.upreyvan.carti.data.local;

import com.upreyvan.carti.model.Debt;
import java.util.ArrayList;
import java.util.List;

public class DebtManager {
    private static DebtManager instance;
    private final List<Debt> debts;
    private OnDebtChangeListener listener;

    public interface OnDebtChangeListener {
        void onDebtsUpdated();
    }

    private DebtManager() {
        debts = new ArrayList<>();
    }

    public static synchronized DebtManager getInstance() {
        if (instance == null) {
            instance = new DebtManager();
        }
        return instance;
    }

    public void setOnDebtChangeListener(OnDebtChangeListener listener) {
        this.listener = listener;
    }

    public void setDebts(List<Debt> newDebts) {
        debts.clear();
        debts.addAll(newDebts);
        if (listener != null) {
            listener.onDebtsUpdated();
        }
    }

    public List<Debt> getDebts() {
        return new ArrayList<>(debts);
    }

    public void clear() {
        debts.clear();
        if (listener != null) {
            listener.onDebtsUpdated();
        }
    }
}
