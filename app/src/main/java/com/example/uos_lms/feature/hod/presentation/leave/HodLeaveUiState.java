package com.example.uos_lms.feature.hod.presentation.leave;

import com.example.uos_lms.core.domain.model.LeaveApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodLeaveUiState {
    @Builder.Default
    private final LeaveReviewTab tab = LeaveReviewTab.PENDING;
    @Builder.Default
    private final List<LeaveApplication> pending = Collections.emptyList();
    @Builder.Default
    private final List<LeaveApplication> all = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String processingLeaveId;
    private final String errorMessage;
    private final String actionMessage;

    public static HodLeaveUiState initial() {
        return HodLeaveUiState.builder().build();
    }

    public List<LeaveApplication> getVisibleLeaves() {
        if (tab == LeaveReviewTab.PENDING) return pending;
        List<LeaveApplication> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return sorted;
    }
}
