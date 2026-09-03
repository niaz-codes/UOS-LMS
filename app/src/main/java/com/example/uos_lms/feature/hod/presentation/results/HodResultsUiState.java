package com.example.uos_lms.feature.hod.presentation.results;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodResultsUiState {
    private final String departmentId;
    @Builder.Default
    private final String departmentName = "";
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static HodResultsUiState initial() {
        return HodResultsUiState.builder().build();
    }
}
