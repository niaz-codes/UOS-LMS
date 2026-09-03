package com.example.uos_lms.core.domain.model;

public enum ExamScheduleStatus {
    DRAFT, PUBLISHED, LOCKED;

    public static ExamScheduleStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (ExamScheduleStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
