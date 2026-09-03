package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizQuestion;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuizResponseDto {
    @SerializedName("_id")
    private String id;
    private String subjectId;
    private String departmentId;
    private String semesterId;
    private String title;
    private String description;
    private List<QuizQuestionResponseDto> questions;
    private int timeLimitMinutes;
    private String dueDate;
    private String createdBy;
    private String createdAt;

    public Quiz toDomain() {
        List<QuizQuestion> mapped = new ArrayList<>();
        if (questions != null) {
            for (QuizQuestionResponseDto dto : questions) mapped.add(dto.toDomain());
        }
        return Quiz.builder()
                .id(id)
                .subjectId(subjectId)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .title(title)
                .description(description)
                .questions(mapped)
                .timeLimitMinutes(timeLimitMinutes)
                .dueDateMillis(IsoDates.toMillis(dueDate))
                .createdBy(createdBy)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
