package com.example.uos_lms.feature.student.presentation.material;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiMaterialDataSource;
import com.example.uos_lms.core.domain.model.StudyMaterial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentSubjectMaterialsViewModel extends ViewModel {

    private final ApiMaterialDataSource materialDataSource;
    private final String subjectId;

    private final MutableLiveData<StudentSubjectMaterialsUiState> uiState =
            new MutableLiveData<>(StudentSubjectMaterialsUiState.initial());

    @Inject
    public StudentSubjectMaterialsViewModel(SavedStateHandle savedStateHandle, ApiMaterialDataSource materialDataSource) {
        this.materialDataSource = materialDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        load();
    }

    public LiveData<StudentSubjectMaterialsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches this subject's materials from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight. */
    public void refresh() {
        StudentSubjectMaterialsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        materialDataSource.materialsForSubject(subjectId)
                .addOnSuccessListener(materials -> {
                    List<StudyMaterial> sorted = new ArrayList<>(materials);
                    sorted.sort(Comparator.comparingLong(StudyMaterial::getUploadedAt).reversed());
                    uiState.setValue(uiState.getValue().toBuilder().materials(sorted).loading(false).refreshing(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }
}
