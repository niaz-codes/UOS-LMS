package com.example.uos_lms.feature.admin.presentation.promotion;

import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class PromotionStudentRow {
    private final User student;
    @Builder.Default
    private final List<ExamResult> failedResults = Collections.emptyList();
    private final boolean hasResults;
    private final boolean alreadyPromoted;
    private final boolean selected;

    public boolean hasFailedSubjects() {
        return !failedResults.isEmpty();
    }
}
