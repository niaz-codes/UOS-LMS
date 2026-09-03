package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.CreateStudyMaterialRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.StudyMaterialResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateStudyMaterialRequestDto;
import com.example.uos_lms.core.domain.model.MaterialType;
import com.example.uos_lms.core.domain.model.StudyMaterial;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreMaterialDataSource. One-shot only. */
@Singleton
public class ApiMaterialDataSource {

    private final MaterialApi materialApi;

    @Inject
    public ApiMaterialDataSource(MaterialApi materialApi) {
        this.materialApi = materialApi;
    }

    public Task<StudyMaterial> uploadMaterial(String subjectId, String title, String description, MaterialType materialType, String mediaId) {
        CreateStudyMaterialRequestDto request = CreateStudyMaterialRequestDto.builder()
                .subjectId(subjectId).title(title).description(description).materialType(materialType.name()).mediaId(mediaId).build();
        return RetrofitTasks.call(materialApi.create(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getMaterial().toDomain()));
    }

    public Task<StudyMaterial> updateMaterial(String materialId, String title, String description, MaterialType materialType) {
        UpdateStudyMaterialRequestDto request = UpdateStudyMaterialRequestDto.builder()
                .title(title).description(description).materialType(materialType.name()).build();
        return RetrofitTasks.call(materialApi.update(materialId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getMaterial().toDomain()));
    }

    public Task<List<StudyMaterial>> materialsForSubject(String subjectId) {
        return RetrofitTasks.call(materialApi.list(subjectId)).onSuccessTask(envelope -> {
            List<StudyMaterial> materials = new ArrayList<>();
            if (envelope.getMaterials() != null) {
                for (StudyMaterialResponseDto dto : envelope.getMaterials()) materials.add(dto.toDomain());
            }
            return Tasks.forResult(materials);
        });
    }

    public Task<Void> deleteMaterial(String materialId) {
        return RetrofitTasks.call(materialApi.remove(materialId));
    }
}
