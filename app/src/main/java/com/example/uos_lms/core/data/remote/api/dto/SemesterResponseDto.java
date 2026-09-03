package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Semester;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SemesterResponseDto {
    @SerializedName("_id")
    private String id;
    private String departmentId;
    private int number;
    private String createdAt;

    public Semester toDomain() {
        return Semester.builder()
                .id(id)
                .departmentId(departmentId)
                .number(number)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
