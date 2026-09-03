package com.example.uos_lms.feature.student.presentation.results;

import com.example.uos_lms.core.domain.model.PromotionStatus;
import com.example.uos_lms.core.domain.model.RepeatStatus;
import com.example.uos_lms.core.domain.model.SubjectResultSnapshot;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentSemesterResultUiState {
    @Builder.Default
    private final boolean hasResult = false;
    @Builder.Default
    private final String semesterLabel = "";
    @Builder.Default
    private final double semesterGpa = 0.0;
    @Builder.Default
    private final double cumulativeCgpa = 0.0;
    @Builder.Default
    private final PromotionStatus promotionStatus = PromotionStatus.NOT_EVALUATED;
    @Builder.Default
    private final List<SubjectResultSnapshot> subjectResults = Collections.emptyList();
    @Builder.Default
    private final Map<String, RepeatStatus> repeatStatusBySubjectId = Collections.emptyMap();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public boolean isAllPassed() {
        return subjectResults.stream().allMatch(s -> s.getStatus() == com.example.uos_lms.core.domain.model.SubjectResultStatus.PASS);
    }

    public static StudentSemesterResultUiState initial() {
        return StudentSemesterResultUiState.builder().build();
    }
}
