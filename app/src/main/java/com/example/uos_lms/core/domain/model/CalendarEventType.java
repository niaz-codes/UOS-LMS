package com.example.uos_lms.core.domain.model;

public enum CalendarEventType {
    HOLIDAY, EVENT, EXAM;

    public static CalendarEventType fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (CalendarEventType type : values()) {
            if (type.name().equals(raw)) return type;
        }
        return null;
    }
}
