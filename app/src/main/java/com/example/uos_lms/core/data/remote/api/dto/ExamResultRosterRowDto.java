package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExamResultRosterRowDto {
    private UserResponseDto student;
    private ExamResultResponseDto result;
    private RepeatExamResponseDto repeatExam;
}
