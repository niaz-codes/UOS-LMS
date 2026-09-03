package com.example.uos_lms.core.domain.model;

/**
 * DRAFT and REJECTED are the only states a Teacher may edit marks in.
 * PENDING_HOD_APPROVAL is reached by the Teacher's "Submit for HOD Approval" action
 * and can only be resolved by an HOD into APPROVED or REJECTED - a Teacher can
 * never self-approve (enforced server-side, see backend/src/controllers/examResultController.js).
 * APPROVED is the final, published state: it is the only status a Student is ever
 * allowed to see. Value names match the backend's ResultStatus enum exactly.
 */
public enum ResultStatus {
    DRAFT, PENDING_HOD_APPROVAL, APPROVED, REJECTED;

    public static ResultStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (ResultStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
