package com.example.uos_lms.core.data.remote.api.dto;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MediaResponseDto {
    @SerializedName("_id")
    private String id;

    private String secureUrl;
    private String publicId;
    private String resourceType;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String category;
    private String uploadedBy;
}
