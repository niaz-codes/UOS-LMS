package com.example.uos_lms.core.domain.model;

public enum UserStatus {
    PENDING, APPROVED, REJECTED, SUSPENDED;

    public static UserStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (UserStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
