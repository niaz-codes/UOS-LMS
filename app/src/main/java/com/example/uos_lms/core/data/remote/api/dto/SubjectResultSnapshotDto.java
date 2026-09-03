package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.domain.model.SubjectResultSnapshot;
import com.example.uos_lms.core.domain.model.SubjectResultStatus;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Maps one entry of StudentSemesterResult.subjectResults (see backend/src/models/
 * StudentSemesterResult.js's subjectResultSnapshotSchema). */
@Data
@NoArgsConstructor
public class SubjectResultSnapshotDto {
    private String subjectId;
    private String courseCode;
    private String subjectName;
    private int creditHours;
    private double marks;
    private String grade;
    private double gpa;
    private String status; // PASS | FAIL

    public SubjectResultSnapshot toDomain() {
        return SubjectResultSnapshot.builder()
                .subjectId(subjectId)
                .courseCode(courseCode)
                .subjectName(subjectName)
                .creditHours(creditHours)
                .marks(marks)
                .grade(grade)
                .gpa(gpa)
                .status(SubjectResultStatus.fromStringOrNull(status))
                .build();
    }
}
