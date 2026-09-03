package com.example.uos_lms.feature.hod.presentation.teacherleave;

import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodTeacherLeaveUiState {
    @Builder.Default
    private final TeacherLeaveReviewTab tab = TeacherLeaveReviewTab.PENDING;
    @Builder.Default
    private final List<TeacherLeaveApplication> pending = Collections.emptyList();
    @Builder.Default
    private final List<TeacherLeaveApplication> all = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String processingLeaveId;
    private final String errorMessage;
    private final String actionMessage;

    public static HodTeacherLeaveUiState initial() {
        return HodTeacherLeaveUiState.builder().build();
    }

    public List<TeacherLeaveApplication> getVisibleLeaves() {
        if (tab == TeacherLeaveReviewTab.PENDING) return pending;
        List<TeacherLeaveApplication> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return sorted;
    }
}
