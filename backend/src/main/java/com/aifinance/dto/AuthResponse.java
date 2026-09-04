package com.aifinance.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String username;
    private String email;
    private String role;
    private long expiresIn;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, String username, String email, String role, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
