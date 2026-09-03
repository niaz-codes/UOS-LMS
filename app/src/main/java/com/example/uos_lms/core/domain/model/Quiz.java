package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Quiz {
    private final String id;
    private final String subjectId;
    private final String departmentId;
    private final String semesterId;
    private final String title;
    private final String description;
    @Builder.Default
    private final List<QuizQuestion> questions = Collections.emptyList();
    private final int timeLimitMinutes;
    private final long dueDateMillis;
    private final String createdBy;
    @Builder.Default
    private final long createdAt = 0L;

    public int getTotalMarks() {
        int total = 0;
        for (QuizQuestion question : questions) {
            total += question.getMarks();
        }
        return total;
    }
}
