package com.upreyvan.carti.models;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class Member {
    @NonNull
    private String id;
    private String familyId;
    private String title;
    private String description;
    private String status;
    private int avatarRes;
    private String avatarUrl;
    private double amount;
    private double totalExpense;
    private boolean isSelected;

    public Member(@NonNull String id, String familyId, String title, String description, String status, int avatarRes, double amount) {
        this.id = id;
        this.familyId = familyId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.avatarRes = avatarRes;
        this.amount = amount;
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAvatarRes() { return avatarRes; }
    public void setAvatarRes(int avatarRes) { this.avatarRes = avatarRes; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Double.compare(member.amount, amount) == 0 &&
                Double.compare(member.totalExpense, totalExpense) == 0 &&
                id.equals(member.id) &&
                isSelected == member.isSelected &&
                Objects.equals(title, member.title) &&
                Objects.equals(description, member.description) &&
                Objects.equals(status, member.status) &&
                Objects.equals(avatarUrl, member.avatarUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, status, amount, totalExpense, avatarUrl, isSelected);
    }

    public static final DiffUtil.ItemCallback<Member> DIFF_CALLBACK = new DiffUtil.ItemCallback<Member>() {
        @Override
        public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }
    };
}
