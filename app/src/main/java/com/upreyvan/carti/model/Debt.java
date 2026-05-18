package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "debts")
public class Debt {
    @PrimaryKey
    @NonNull
    private String id;
    private String familyId;
    private String personName;
    private String description;
    private String date;
    private double amount;
    private boolean isPaid;
    private int avatarResId;
    private String notes;

    @Ignore
    public Debt() {
        this.id = "";
        this.familyId = "";
        this.personName = "";
    }

    public Debt(@NonNull String id, String familyId, String personName, String description, String date, double amount, boolean isPaid, int avatarResId, String notes) {
        this.id = id;
        this.familyId = familyId;
        this.personName = personName;
        this.description = description;
        this.date = date;
        this.amount = amount;
        this.isPaid = isPaid;
        this.avatarResId = avatarResId;
        this.notes = notes;
    }

    @NonNull
    public String getId() { return id; }
    public String getFamilyId() { return familyId; }
    public void setId(@NonNull String id) { this.id = id; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public void setPersonName(String personName) { this.personName = personName; }
    public void setDescription(String description) { this.description = description; }
    public void setDate(String date) { this.date = date; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setPaid(boolean paid) { isPaid = paid; }
    public void setAvatarResId(int avatarResId) { this.avatarResId = avatarResId; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPersonName() { return personName; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public boolean isPaid() { return isPaid; }
    public int getAvatarResId() { return avatarResId; }
    public String getNotes() { return notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Debt debt = (Debt) o;
        return Double.compare(debt.amount, amount) == 0 &&
                isPaid == debt.isPaid &&
                id.equals(debt.id) &&
                java.util.Objects.equals(personName, debt.personName) &&
                java.util.Objects.equals(description, debt.description);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, personName, description, amount, isPaid);
    }

    public static final androidx.recyclerview.widget.DiffUtil.ItemCallback<Debt> DIFF_CALLBACK =
            new androidx.recyclerview.widget.DiffUtil.ItemCallback<Debt>() {
                @Override
                public boolean areItemsTheSame(@androidx.annotation.NonNull Debt oldItem, @androidx.annotation.NonNull Debt newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@androidx.annotation.NonNull Debt oldItem, @androidx.annotation.NonNull Debt newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
