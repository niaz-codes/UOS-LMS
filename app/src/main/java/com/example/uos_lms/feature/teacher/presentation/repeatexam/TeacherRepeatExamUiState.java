package com.example.uos_lms.feature.teacher.presentation.repeatexam;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherRepeatExamUiState {
    @Builder.Default
    private final List<RepeatExamRow> rows = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String processingExamResultId;
    private final String errorMessage;
    private final String actionMessage;

    public static TeacherRepeatExamUiState initial() {
        return TeacherRepeatExamUiState.builder().build();
    }
}
