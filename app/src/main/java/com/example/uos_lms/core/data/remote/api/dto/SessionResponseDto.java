package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Session;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SessionResponseDto {
    @SerializedName("_id")
    private String id;
    private String departmentId;
    private String label;
    private boolean isActive;
    private String createdAt;

    public Session toDomain() {
        return Session.builder()
                .id(id)
                .departmentId(departmentId)
                .label(label)
                .isActive(isActive)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
