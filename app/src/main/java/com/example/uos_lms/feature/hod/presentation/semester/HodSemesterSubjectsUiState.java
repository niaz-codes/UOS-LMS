package com.example.uos_lms.feature.hod.presentation.semester;

import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodSemesterSubjectsUiState {
    private final Semester semester;
    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    @Builder.Default
    private final List<User> teachers = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static HodSemesterSubjectsUiState initial() {
        return HodSemesterSubjectsUiState.builder().build();
    }
}
