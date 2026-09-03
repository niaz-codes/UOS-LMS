package com.example.uos_lms.feature.teacher.presentation.dashboard;

import com.example.uos_lms.core.domain.model.Subject;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssignedSubject {
    private final Subject subject;
    private final String departmentName;
    private final int semesterNumber;
    private final String semesterLabel;
    private final String sessionLabel;
}
