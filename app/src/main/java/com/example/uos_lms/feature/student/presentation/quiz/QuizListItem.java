package com.example.uos_lms.feature.student.presentation.quiz;

import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAttempt;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizListItem {
    private final Quiz quiz;
    private final QuizAttempt attempt;
}
