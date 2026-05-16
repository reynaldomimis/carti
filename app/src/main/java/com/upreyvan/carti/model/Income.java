package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "incomes")
public class Income {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String familyId;
    private String source; // e.g., Salary, Business, Freelance
    private double amount;
    private String createdAt; // ISO timestamp from Appwrite

    public Income(@NonNull String id, String userId, String familyId, String source, double amount, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.source = source;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFamilyId() { return familyId; }
    public String getSource() { return source; }
    public double getAmount() { return amount; }
    public String getCreatedAt() { return createdAt; }

    public void setId(@NonNull String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public void setSource(String source) { this.source = source; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}