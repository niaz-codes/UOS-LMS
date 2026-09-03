package com.example.uos_lms.core.domain.model;

/**
 * PENDING: scheduled, no marks entered yet. SUBMITTED: teacher entered new marks, awaiting
 * HOD review. APPROVED: original ExamResult updated in place with the new marks (see
 * backend/src/controllers/repeatExamController.js approve()). REJECTED: back to the
 * teacher's desk with a reason, marks re-enterable.
 */
public enum RepeatStatus {
    PENDING, SUBMITTED, APPROVED, REJECTED;

    public static RepeatStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (RepeatStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
