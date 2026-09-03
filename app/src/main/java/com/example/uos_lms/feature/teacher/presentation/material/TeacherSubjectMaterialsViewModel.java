package com.example.uos_lms.feature.teacher.presentation.material;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiMaterialDataSource;
import com.example.uos_lms.core.data.remote.api.ApiMediaDataSource;
import com.example.uos_lms.core.data.remote.api.dto.MediaResponseDto;
import com.example.uos_lms.core.domain.model.MaterialType;
import com.example.uos_lms.core.domain.model.StudyMaterial;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherSubjectMaterialsViewModel extends ViewModel {

    private static final String MEDIA_CATEGORY = "study_material";

    private final ApiMaterialDataSource materialDataSource;
    private final ApiMediaDataSource mediaDataSource;
    private final String subjectId;

    private final MutableLiveData<TeacherSubjectMaterialsUiState> uiState =
            new MutableLiveData<>(TeacherSubjectMaterialsUiState.initial());

    @Inject
    public TeacherSubjectMaterialsViewModel(
            SavedStateHandle savedStateHandle,
            ApiMaterialDataSource materialDataSource,
            ApiMediaDataSource mediaDataSource) {
        this.materialDataSource = materialDataSource;
        this.mediaDataSource = mediaDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
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

    public LiveData<TeacherSubjectMaterialsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches this subject's materials from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight. */
    public void refresh() {
        TeacherSubjectMaterialsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    public void upload(String title, String description, MaterialType materialType, Uri fileUri) {
        uiState.setValue(uiState.getValue().toBuilder().uploading(true).uploadProgress(null).errorMessage(null).build());

        Task<MediaResponseDto> uploadTask = mediaDataSource.upload(MEDIA_CATEGORY, "subject", subjectId, fileUri,
                percent -> uiState.postValue(uiState.getValue().toBuilder().uploadProgress(percent).build()));

        uploadTask.continueWithTask(mediaResultTask ->
                materialDataSource.uploadMaterial(subjectId, title, description, materialType, mediaResultTask.getResult().getId())
        ).addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().uploading(false).uploadProgress(null).build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .uploading(false).uploadProgress(null).errorMessage(e.getMessage()).build()));
    }

    /** Metadata-only - see ApiMaterialDataSource.updateMaterial / materialController.update. */
    public void update(String materialId, String title, String description, MaterialType materialType) {
        materialDataSource.updateMaterial(materialId, title, description, materialType)
                .addOnSuccessListener(v -> load())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void delete(String materialId) {
        materialDataSource.deleteMaterial(materialId)
                .addOnSuccessListener(v -> load())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }
}
