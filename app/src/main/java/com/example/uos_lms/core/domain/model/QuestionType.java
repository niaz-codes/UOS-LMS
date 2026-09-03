package com.example.uos_lms.core.domain.model;

public enum QuestionType {
    MCQ, TEXT;

    public static QuestionType fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (QuestionType type : values()) {
            if (type.name().equals(raw)) return type;
        }
        return null;
    }
}
