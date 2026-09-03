package com.example.uos_lms.feature.admin.university.presentation.department;

import com.example.uos_lms.core.domain.model.Session;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionDeleteCheck {
    private final Session session;
    private final long linkedStudentCount;
}
