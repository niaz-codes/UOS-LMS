package com.example.uos_lms.core.domain.model;

public enum AttendanceStatus {
    PRESENT, ABSENT;

    public static AttendanceStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (AttendanceStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
