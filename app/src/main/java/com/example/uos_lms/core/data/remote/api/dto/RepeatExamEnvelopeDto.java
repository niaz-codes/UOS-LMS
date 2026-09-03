package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RepeatExamEnvelopeDto {
    private RepeatExamResponseDto repeatExam;
    private ExamResultResponseDto examResult;
}
