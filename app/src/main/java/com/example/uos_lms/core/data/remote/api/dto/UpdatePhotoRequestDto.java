package com.example.uos_lms.core.data.remote.api.dto;

import androidx.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdatePhotoRequestDto {
    @Nullable
    private final String profilePhotoUrl;
    @Nullable
    private final String profilePhotoPublicId;
}
