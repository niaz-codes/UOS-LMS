package com.example.uos_lms.core.data.remote.api.dto;

/** SubjectResponseDto plus the roster size the backend already computed while building the
 * "my students" union (subjectController.myStudents) - avoids a second per-subject count call. */
public class TeacherSubjectSummaryDto extends SubjectResponseDto {
    private int studentCount;

    public int getStudentCount() {
        return studentCount;
    }
}
