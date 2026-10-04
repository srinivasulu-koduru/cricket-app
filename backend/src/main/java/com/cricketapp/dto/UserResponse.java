package com.cricketapp.dto;

public class UserResponse {

    private String userId;
    private String name;
    private String email;
    private boolean emailVerified;

    public UserResponse() {
    }

    public UserResponse(String userId, String name, String email, boolean emailVerified) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.emailVerified = emailVerified;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
