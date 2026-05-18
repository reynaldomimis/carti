package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "incomes")
public class Income {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String familyId;
    private String source;
    private double amount;
    private String createdAt; 

    @Ignore
    public Income() {
        this.id = "";
        this.userId = "";
        this.familyId = "";
        this.source = "";
        this.amount = 0.0;
        this.createdAt = "";
    }

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
    public void setId(@NonNull String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Income income = (Income) o;
        return Double.compare(income.amount, amount) == 0 &&
                id.equals(income.id) &&
                Objects.equals(source, income.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, source, amount);
    }

    public static final DiffUtil.ItemCallback<Income> DIFF_CALLBACK = new DiffUtil.ItemCallback<Income>() {
        @Override
        public boolean areItemsTheSame(@NonNull Income oldItem, @NonNull Income newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Income oldItem, @NonNull Income newItem) {
            return oldItem.equals(newItem);
        }
    };
}
