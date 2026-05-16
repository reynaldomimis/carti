package com.upreyvan.carti.model;

public class Member {
    private String id;
    private String name;
    private String role;
    private String status;
    private int avatarRes;

    public Member(String id, String name, String role, String status, int avatarRes) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.status = status;
        this.avatarRes = avatarRes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public int getAvatarRes() { return avatarRes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return id != null ? id.equals(member.id) : member.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}