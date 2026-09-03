package com.example.uos_lms.core.domain.model;

/**
 * Per-semester promotion state, computed server-side by recalculateSemesterGpa alongside
 * GPA/CGPA (see backend/src/services/recalculateSemesterGpa.js). NOT_EVALUATED until the
 * semester's results are fully APPROVED; ELIGIBLE_FOR_PROMOTION once approved with fewer
 * than MAX_ALLOWED_FAILED_SUBJECTS fails; NOT_PROMOTED if 4+ subjects failed; PROMOTED once
 * an HOD/Admin has actually run the promotion action. Value names match the backend's
 * PromotionStatus enum exactly.
 */
public enum PromotionStatus {
    NOT_EVALUATED, ELIGIBLE_FOR_PROMOTION, NOT_PROMOTED, PROMOTED;

    public static PromotionStatus fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (PromotionStatus status : values()) {
            if (status.name().equals(raw)) return status;
        }
        return null;
    }
}
