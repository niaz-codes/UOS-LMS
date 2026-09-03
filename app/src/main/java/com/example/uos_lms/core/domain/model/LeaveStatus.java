package com.example.uos_lms.core.domain.model;

public enum LeaveStatus {
    PENDING, APPROVED, REJECTED;

    public static LeaveStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (LeaveStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
