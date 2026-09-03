package com.example.uos_lms.core.domain.model;

import java.util.Locale;

public enum DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    public static DayOfWeek fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (DayOfWeek day : values()) {
            if (day.name().equals(raw)) return day;
        }
        return null;
    }

    public String getDisplayName() {
        String lower = name().toLowerCase(Locale.US);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
