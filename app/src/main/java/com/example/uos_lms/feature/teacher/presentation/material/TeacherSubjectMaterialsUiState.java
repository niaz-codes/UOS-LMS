package com.example.uos_lms.feature.teacher.presentation.material;

import com.example.uos_lms.core.domain.model.StudyMaterial;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherSubjectMaterialsUiState {
    @Builder.Default
    private final List<StudyMaterial> materials = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    @Builder.Default
    private final boolean uploading = false;
    private final Integer uploadProgress;
    private final String errorMessage;

    public static TeacherSubjectMaterialsUiState initial() {
        return TeacherSubjectMaterialsUiState.builder().build();
    }
}
