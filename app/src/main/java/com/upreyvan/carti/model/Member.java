package com.upreyvan.carti.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "members")
public class Member {
    @PrimaryKey
    @NonNull
    private String id;
    private String familyId;
    private String name;
    private String role;
    private String status;
    private int avatarRes;
    private double salary;

    public Member(@NonNull String id, String familyId, String name, String role, String status, int avatarRes, double salary) {
        this.id = id;
        this.familyId = familyId;
        this.name = name;
        this.role = role;
        this.status = status;
        this.avatarRes = avatarRes;
        this.salary = salary;
    }

    @NonNull
    public String getId() { return id; }
    public String getFamilyId() { return familyId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public int getAvatarRes() { return avatarRes; }
    public double getSalary() { return salary; }

    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public void setSalary(double salary) { this.salary = salary; }
}