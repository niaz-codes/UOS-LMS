package com.example.uos_lms.feature.hod.presentation.repeatexam;

import com.example.uos_lms.core.domain.model.RepeatExam;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodRepeatExamUiState {
    @Builder.Default
    private final List<RepeatExam> pending = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String processingRepeatExamId;
    private final String errorMessage;
    private final String actionMessage;

    public static HodRepeatExamUiState initial() {
        return HodRepeatExamUiState.builder().build();
    }
}
