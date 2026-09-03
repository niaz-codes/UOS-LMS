package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AssignmentSubmission {
    private final String id;
    private final String assignmentId;
    private final String subjectId;
    private final String studentUid;
    private final String studentName;
    private final String textAnswer;
    private final String fileUrl;
    private final String fileName;
    private final String filePublicId;
    private final String fileResourceType;
    private final Long fileSize;
    @Builder.Default
    private final long submittedAt = 0L;
    private final Integer marksObtained;
    private final String feedback;
    private final String gradedBy;
    private final Long gradedAt;

    public boolean isGraded() {
        return marksObtained != null;
    }
}
