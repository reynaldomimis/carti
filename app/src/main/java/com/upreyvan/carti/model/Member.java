package com.upreyvan.carti.model;

public class Member {
    private String name;
    private String role;
    private int avatarRes;

    public Member(String name, String role, int avatarRes) {
        this.name = name;
        this.role = role;
        this.avatarRes = avatarRes;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public int getAvatarRes() { return avatarRes; }
}