package com.example.uos_lms.feature.student.presentation.submissions;

import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubmissionRow {
    private final Assignment assignment;
    private final AssignmentSubmission submission;
}
