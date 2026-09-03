package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateStudyMaterialRequestDto {
    private final String subjectId;
    private final String title;
    private final String description;
    private final String materialType;
    private final String mediaId;
}
