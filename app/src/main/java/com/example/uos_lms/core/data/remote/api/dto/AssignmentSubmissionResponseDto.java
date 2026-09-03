package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AssignmentSubmissionResponseDto {
    @SerializedName("_id")
    private String id;
    private String assignmentId;
    private String subjectId;
    private PersonRefDto studentId;
    private String textAnswer;
    private String fileUrl;
    private String fileName;
    private String filePublicId;
    private String fileResourceType;
    private Long fileSize;
    private String submittedAt;
    private Integer marksObtained;
    private String feedback;
    private String gradedBy;
    private String gradedAt;

    public AssignmentSubmission toDomain() {
        return AssignmentSubmission.builder()
                .id(id)
                .assignmentId(assignmentId)
                .subjectId(subjectId)
                .studentUid(studentId != null ? studentId.getId() : null)
                .studentName(studentId != null ? studentId.getFullName() : null)
                .textAnswer(textAnswer)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .filePublicId(filePublicId)
                .fileResourceType(fileResourceType)
                .fileSize(fileSize)
                .submittedAt(IsoDates.toMillis(submittedAt))
                .marksObtained(marksObtained)
                .feedback(feedback)
                .gradedBy(gradedBy)
                .gradedAt(gradedAt != null ? IsoDates.toMillis(gradedAt) : null)
                .build();
    }
}
