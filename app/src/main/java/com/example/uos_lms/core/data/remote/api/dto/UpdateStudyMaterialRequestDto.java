package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

/** Metadata-only edit (title/description/materialType) - see materialController.update. */
@Data
@Builder
public class UpdateStudyMaterialRequestDto {
    private final String title;
    private final String description;
    private final String materialType;
}
