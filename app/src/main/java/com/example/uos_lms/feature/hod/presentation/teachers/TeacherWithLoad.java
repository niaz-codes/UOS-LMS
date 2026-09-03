package com.example.uos_lms.feature.hod.presentation.teachers;

import com.example.uos_lms.core.domain.model.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherWithLoad {
    private final User teacher;
    private final int subjectCount;
}
