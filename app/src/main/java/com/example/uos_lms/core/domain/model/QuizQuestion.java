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
public class QuizQuestion {
    private final String text;
    @Builder.Default
    private final QuestionType questionType = QuestionType.MCQ;
    @Builder.Default
    private final List<String> options = Collections.emptyList();
    private final int correctOptionIndex;
    private final int marks;

    public boolean isText() {
        return questionType == QuestionType.TEXT;
    }
}
