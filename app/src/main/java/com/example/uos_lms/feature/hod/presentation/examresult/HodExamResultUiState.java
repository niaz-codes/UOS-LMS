package com.example.uos_lms.feature.hod.presentation.examresult;

import com.example.uos_lms.core.domain.model.ExamResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodExamResultUiState {
    @Builder.Default
    private final ResultReviewTab tab = ResultReviewTab.PENDING;
    @Builder.Default
    private final List<ExamResult> pending = Collections.emptyList();
    @Builder.Default
    private final List<ExamResult> all = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String processingResultId;
    private final String errorMessage;
    private final String actionMessage;

    public static HodExamResultUiState initial() {
        return HodExamResultUiState.builder().build();
    }

    public List<ExamResult> getVisibleResults() {
        if (tab == ResultReviewTab.PENDING) return pending;
        List<ExamResult> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return sorted;
    }
}
