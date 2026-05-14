package com.upreyvan.carti.model;

public class Debt {
    private String id;
    private String personName;
    private String description;
    private String date;
    private double amount;
    private boolean isPaid;
    private int avatarResId;
    private String notes;

    public Debt(String id, String personName, String description, String date, double amount, boolean isPaid, int avatarResId, String notes) {
        this.id = id;
        this.personName = personName;
        this.description = description;
        this.date = date;
        this.amount = amount;
        this.isPaid = isPaid;
        this.avatarResId = avatarResId;
        this.notes = notes;
    }

    public String getId() { return id; }
    public String getPersonName() { return personName; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public boolean isPaid() { return isPaid; }
    public int getAvatarResId() { return avatarResId; }
    public String getNotes() { return notes; }

    public void setPaid(boolean paid) { isPaid = paid; }
}