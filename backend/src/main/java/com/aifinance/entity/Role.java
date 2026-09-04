package com.aifinance.entity;

public enum Role {
    ROLE_ADMIN,
    ROLE_ANALYST,
    ROLE_VIEWER;

    public String getAuthorityName() {
        return name().replace("ROLE_", "");
    }
}
