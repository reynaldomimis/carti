package com.upreyvan.carti.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upreyvan.carti.model.Debt;
import com.upreyvan.carti.util.Constants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DebtManager {
    private static DebtManager instance;
    private final List<Debt> debts;
    private final SharedPreferences prefs;
    private final Gson gson;
    private OnDebtChangeListener listener;

    public interface OnDebtChangeListener {
        void onDebtsUpdated();
    }

    private DebtManager(Context context) {
        prefs = context.getSharedPreferences(Constants.Keys.PREF_DEBT, Context.MODE_PRIVATE);
        gson = new Gson();
        debts = loadDebts();
    }

    public static synchronized DebtManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("DebtManager must be initialized with Context first");
        }
        return instance;
    }

    public static synchronized DebtManager init(Context context) {
        if (instance == null) {
            instance = new DebtManager(context.getApplicationContext());
        }
        return instance;
    }

    private List<Debt> loadDebts() {
        String json = prefs.getString(Constants.Keys.KEY_DEBTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<Debt>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveDebts() {
        prefs.edit().putString(Constants.Keys.KEY_DEBTS, gson.toJson(debts)).apply();
    }

    public void setOnDebtChangeListener(OnDebtChangeListener listener) {
        this.listener = listener;
    }

    public void addDebt(Debt debt) {
        debts.add(0, debt);
        saveDebts();
        if (listener != null) {
            listener.onDebtsUpdated();
        }
    }

    public void setDebts(List<Debt> newDebts) {
        debts.clear();
        debts.addAll(newDebts);
        saveDebts();
        if (listener != null) {
            listener.onDebtsUpdated();
        }
    }

    public List<Debt> getDebts() {
        return new ArrayList<>(debts);
    }

    public void clear() {
        debts.clear();
        saveDebts();
        if (listener != null) {
            listener.onDebtsUpdated();
        }
    }
}
