package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.domain.model.RepeatStatus;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RepeatExamResponseDto {
    @SerializedName("_id")
    private String id;

    private String examResultId;
    private SubjectRefDto subjectId;
    private PersonRefDto studentId;

    private double previousMarks;
    private String previousGrade;
    private double previousGpa;

    private Double newMarks;
    private String newGrade;
    private Double newGpa;

    private String repeatStatus;
    private String rejectionReason;
    private String createdAt;

    public RepeatExam toDomain() {
        return RepeatExam.builder()
                .id(id)
                .examResultId(examResultId)
                .studentUid(studentId != null ? studentId.getId() : null)
                .studentName(studentId != null ? studentId.getFullName() : "")
                .studentRollNumber(studentId != null ? studentId.getRollNumber() : null)
                .subjectId(subjectId != null ? subjectId.getId() : null)
                .subjectCode(subjectId != null ? subjectId.getCode() : "")
                .subjectTitle(subjectId != null ? subjectId.getTitle() : "")
                .previousMarks(previousMarks)
                .previousGrade(previousGrade)
                .previousGpa(previousGpa)
                .newMarks(newMarks)
                .newGrade(newGrade)
                .newGpa(newGpa)
                .repeatStatus(RepeatStatus.fromStringOrNull(repeatStatus))
                .rejectionReason(rejectionReason)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
