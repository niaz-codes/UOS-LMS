package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.domain.model.Department;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DepartmentResponseDto {
    @SerializedName("_id")
    private String id;
    private String name;
    private String code;
    private String description;
    private String createdAt;

    public Department toDomain() {
        return Department.builder()
                .id(id)
                .name(name)
                .code(code)
                .description(description != null ? description : "")
                .createdAt(com.example.uos_lms.core.data.remote.api.IsoDates.toMillis(createdAt))
                .build();
    }
}
