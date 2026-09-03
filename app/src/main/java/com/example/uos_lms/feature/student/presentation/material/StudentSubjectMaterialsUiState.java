package com.example.uos_lms.feature.student.presentation.material;

import com.example.uos_lms.core.domain.model.StudyMaterial;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentSubjectMaterialsUiState {
    @Builder.Default
    private final List<StudyMaterial> materials = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static StudentSubjectMaterialsUiState initial() {
        return StudentSubjectMaterialsUiState.builder().build();
    }
}
