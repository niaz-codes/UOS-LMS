package com.example.uos_lms.feature.admin.presentation.leave;

import com.example.uos_lms.core.domain.model.LeaveApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminLeaveUiState {
    @Builder.Default
    private final List<LeaveApplication> leaves = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String processingLeaveId;
    private final String errorMessage;
    private final String actionMessage;

    public static AdminLeaveUiState initial() {
        return AdminLeaveUiState.builder().build();
    }

    public List<LeaveApplication> getSortedLeaves() {
        List<LeaveApplication> sorted = new ArrayList<>(leaves);
        sorted.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return sorted;
    }
}
