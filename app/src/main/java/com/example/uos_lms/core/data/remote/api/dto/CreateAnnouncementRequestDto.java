package com.example.uos_lms.core.data.remote.api.dto;

import androidx.annotation.Nullable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateAnnouncementRequestDto {
    private final String title;
    private final String body;
    @Nullable
    private final String scope;
    @Nullable
    private final String departmentId;
    @Nullable
    private final String subjectId;
}
