package com.gateway.api_gateway.dto;

public class LoginRequest {
    private String username;
    private String password;  // even if you don't validate yet, include it

    // No-args constructor (required for JSON deserialization)
    public LoginRequest() {}

    // All-args constructor (optional, convenient)
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
