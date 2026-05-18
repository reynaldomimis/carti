package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "debts")
public class Debt {
    @PrimaryKey
    @NonNull
    private String id;
    private String familyId;
    private String title;
    private String description;
    private String timestamp;
    private double amount;
    private boolean isPaid;
    private int avatarResId;
    private String notes;

    @Ignore
    public Debt() {
        this.id = "";
        this.familyId = "";
        this.title = "Title";
        this.description = "Description";
        this.timestamp = "";
        this.amount = 0.00;
        this.isPaid = false;
    }

    public Debt(@NonNull String id, String familyId, String title, String description, String timestamp, double amount, boolean isPaid, int avatarResId, String notes) {
        this.id = id;
        this.familyId = familyId;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.amount = amount;
        this.isPaid = isPaid;
        this.avatarResId = avatarResId;
        this.notes = notes;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    public int getAvatarResId() { return avatarResId; }
    public void setAvatarResId(int avatarResId) { this.avatarResId = avatarResId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPersonName() { return title; }
    public String getDate() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Debt debt = (Debt) o;
        return Double.compare(debt.amount, amount) == 0 &&
                isPaid == debt.isPaid &&
                id.equals(debt.id) &&
                Objects.equals(title, debt.title) &&
                Objects.equals(description, debt.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, amount, isPaid);
    }

    public static final DiffUtil.ItemCallback<Debt> DIFF_CALLBACK = new DiffUtil.ItemCallback<Debt>() {
        @Override
        public boolean areItemsTheSame(@NonNull Debt oldItem, @NonNull Debt newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Debt oldItem, @NonNull Debt newItem) {
            return oldItem.equals(newItem);
        }
    };
}
