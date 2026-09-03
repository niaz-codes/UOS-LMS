package com.example.uos_lms.core.data.remote.api.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExamResultDepartmentOptionDto {
    @SerializedName("_id")
    private String id;
    private String name;
    private String code;
    private List<SessionResponseDto> sessions;
    private List<SemesterResponseDto> semesters;

    public List<SessionResponseDto> getSessions() {
        return sessions != null ? sessions : Collections.emptyList();
    }

    public List<SemesterResponseDto> getSemesters() {
        return semesters != null ? semesters : Collections.emptyList();
    }
}
