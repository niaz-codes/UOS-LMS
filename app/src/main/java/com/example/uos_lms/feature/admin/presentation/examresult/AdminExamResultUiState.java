package com.example.uos_lms.feature.admin.presentation.examresult;

import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ResultStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminExamResultUiState {
    @Builder.Default
    private final List<ExamResult> results = Collections.emptyList();
    private final ResultStatus statusFilter;
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static AdminExamResultUiState initial() {
        return AdminExamResultUiState.builder().build();
    }

    public List<ExamResult> getVisibleResults() {
        List<ExamResult> filtered = new ArrayList<>();
        for (ExamResult result : results) {
            if (statusFilter == null || result.getStatus() == statusFilter) filtered.add(result);
        }
        filtered.sort(Comparator.comparingLong(ExamResult::getUpdatedAt).reversed());
        return filtered;
    }

    public Map<ResultStatus, Integer> getCounts() {
        Map<ResultStatus, Integer> counts = new EnumMap<>(ResultStatus.class);
        for (ResultStatus status : ResultStatus.values()) {
            counts.put(status, 0);
        }
        for (ExamResult result : results) {
            counts.merge(result.getStatus(), 1, Integer::sum);
        }
        return counts;
    }
}
