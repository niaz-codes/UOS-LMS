package com.example.uos_lms.core.domain.model;

public enum AnnouncementScope {
    ALL, DEPARTMENT, SUBJECT;

    public static AnnouncementScope fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (AnnouncementScope scope : values()) {
            if (scope.name().equals(raw)) return scope;
        }
        return null;
    }
}
