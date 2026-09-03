package com.example.uos_lms.feature.hod.presentation.teachers;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodTeachersUiState {
    @Builder.Default
    private final List<TeacherWithLoad> teachers = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static HodTeachersUiState initial() {
        return HodTeachersUiState.builder().build();
    }
}
