package com.example.uos_lms.core.domain.model;

public enum LeaveType {
    CASUAL, SICK, ANNUAL, OTHER;

    public static LeaveType fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (LeaveType type : values()) {
            if (type.name().equals(raw)) return type;
        }
        return null;
    }
}
