package com.upreyvan.carti.models;

public enum TransactionType {
    INCOME,
    ALLOCATION,
    DEBT,
    GOAL,
    EXPENSE;

    public String value() {
        return name();
    }
}
