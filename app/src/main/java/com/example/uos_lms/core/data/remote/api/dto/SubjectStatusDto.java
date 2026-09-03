package com.example.uos_lms.core.data.remote.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One course's teacher-attendance status for a save request - teacherId is deliberately not
 * sent, the backend resolves it from the subject's own assigned teacher. */
@Data
@AllArgsConstructor
public class SubjectStatusDto {
    private final String subjectId;
    private final String status;
}
