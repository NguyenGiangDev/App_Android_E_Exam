package com.example.e_exam.user;

public class User {
    private String uid;
    private String fullName;
    private String birthday;
    private String email;
    private String role;

    public User() {}

    public User(String uid, String fullName, String birthday, String email, String role) {
        this.uid = uid;
        this.fullName = fullName;
        this.birthday = birthday;
        this.email = email;
        this.role = role;
    }

    // Getters và setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
