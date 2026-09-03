package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Maps the backend's ExamResult shape (see backend/src/models/ExamResult.js) onto the
 * pre-existing {@link ExamResult} domain model, so the Teacher/HOD/Student/Admin exam-result
 * screens (UiState classes, Fragments, layouts) built for the Firestore version keep working
 * almost unchanged - only the ViewModels' data-loading logic actually needed to change. */
@Data
@NoArgsConstructor
public class ExamResultResponseDto {
    @SerializedName("_id")
    private String id;

    private SubjectRefDto subjectId;
    private PersonRefDto studentId;
    private PersonRefDto submittedBy;

    private String departmentId;
    private String semesterId;

    private double marks;
    private String grade;
    private double gpa;
    private String status; // PASS | FAIL

    private String resultStatus; // DRAFT | PENDING_HOD_APPROVAL | APPROVED | REJECTED
    private String rejectionReason;

    private boolean repeatEligible;
    private String reviewedBy;
    private String reviewedAt;
    private String createdAt;
    private String updatedAt;

    public ExamResult toDomain() {
        return ExamResult.builder()
                .id(id)
                .subjectId(subjectId != null ? subjectId.getId() : null)
                .subjectCode(subjectId != null ? subjectId.getCode() : "")
                .subjectTitle(subjectId != null ? subjectId.getTitle() : "")
                .creditHours(subjectId != null && subjectId.getCreditHours() != null ? subjectId.getCreditHours() : 0)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .teacherUid(submittedBy != null ? submittedBy.getId() : null)
                .teacherName(submittedBy != null ? submittedBy.getFullName() : "")
                .studentUid(studentId != null ? studentId.getId() : null)
                .studentName(studentId != null ? studentId.getFullName() : "")
                .studentRollNumber(studentId != null ? studentId.getRollNumber() : null)
                .obtainedMarks((int) Math.round(marks))
                .totalMarks(100)
                .status(ResultStatus.fromStringOrNull(resultStatus))
                .grade(grade)
                .gpaPoint(gpa)
                .rejectionReason(rejectionReason)
                .createdAt(IsoDates.toMillis(createdAt))
                .updatedAt(IsoDates.toMillis(updatedAt))
                .reviewedBy(reviewedBy)
                .reviewedAt(reviewedAt != null ? IsoDates.toMillis(reviewedAt) : null)
                .build();
    }
}
