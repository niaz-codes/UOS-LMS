package com.example.uos_lms.core.domain.model;

/** Per-subject pass/fail verdict within a StudentSemesterResult snapshot - separate from
 * ResultStatus (the DRAFT/PENDING/APPROVED/REJECTED workflow state of the whole semester).
 * Value names match the backend's SubjectResultStatus enum exactly. */
public enum SubjectResultStatus {
    PASS, FAIL;

    public static SubjectResultStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (SubjectResultStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
