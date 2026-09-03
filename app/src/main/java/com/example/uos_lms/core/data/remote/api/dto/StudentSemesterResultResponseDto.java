package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.PromotionStatus;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;
import com.example.uos_lms.core.domain.model.SubjectResultSnapshot;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Maps the backend's StudentSemesterResult shape (see backend/src/models/
 * StudentSemesterResult.js and examResultController.js's listSemesterResults) onto
 * {@link StudentSemesterResultSummary} - the Results section's one data source. */
@Data
@NoArgsConstructor
public class StudentSemesterResultResponseDto {
    @SerializedName("_id")
    private String id;

    private PersonRefDto studentId;
    private SemesterResponseDto semesterId;
    private SessionResponseDto sessionId;
    private String departmentId;

    private List<SubjectResultSnapshotDto> subjectResults;
    private double semesterGPA;
    private Double previousSemesterGPA;
    private double cumulativeCGPA;

    private String resultStatus; // DRAFT | PENDING_HOD_APPROVAL | APPROVED | REJECTED
    private String promotionStatus; // NOT_EVALUATED | ELIGIBLE_FOR_PROMOTION | NOT_PROMOTED | PROMOTED
    private List<String> failedSubjectIds;

    private String createdAt;
    private String updatedAt;

    public StudentSemesterResultSummary toDomain() {
        List<SubjectResultSnapshot> results = new ArrayList<>();
        if (subjectResults != null) {
            for (SubjectResultSnapshotDto dto : subjectResults) results.add(dto.toDomain());
        }
        return StudentSemesterResultSummary.builder()
                .id(id)
                .studentUid(studentId != null ? studentId.getId() : null)
                .studentName(studentId != null ? studentId.getFullName() : "")
                .studentRollNumber(studentId != null ? studentId.getRollNumber() : null)
                .departmentId(departmentId)
                .semesterId(semesterId != null ? semesterId.getId() : null)
                .semesterNumber(semesterId != null ? semesterId.getNumber() : 0)
                .sessionId(sessionId != null ? sessionId.getId() : null)
                .sessionLabel(sessionId != null ? sessionId.getLabel() : "")
                .subjectResults(results)
                .semesterGpa(semesterGPA)
                .previousSemesterGpa(previousSemesterGPA)
                .cumulativeCgpa(cumulativeCGPA)
                .resultStatus(ResultStatus.fromStringOrNull(resultStatus))
                .promotionStatus(PromotionStatus.fromStringOrNull(promotionStatus))
                .failedSubjectIds(failedSubjectIds != null ? failedSubjectIds : new ArrayList<>())
                .createdAt(IsoDates.toMillis(createdAt))
                .updatedAt(IsoDates.toMillis(updatedAt))
                .build();
    }
}
