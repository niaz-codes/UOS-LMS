package com.example.uos_lms.core.data.remote.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentStatusDto {
    private final String studentId;
    private final String status;
}
