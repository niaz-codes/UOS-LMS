package com.example.uos_lms.core.domain.model;

public enum ExamType {
    MID_TERM, FINAL_TERM;

    public static ExamType fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (ExamType type : values()) {
            if (type.name().equals(raw)) return type;
        }
        return null;
    }
}
