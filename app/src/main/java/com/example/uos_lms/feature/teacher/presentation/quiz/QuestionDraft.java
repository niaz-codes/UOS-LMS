package com.example.uos_lms.feature.teacher.presentation.quiz;

import com.example.uos_lms.core.domain.model.QuestionType;

import java.util.Arrays;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class QuestionDraft {
    @Builder.Default
    private final QuestionType questionType = QuestionType.MCQ;
    @Builder.Default
    private final String text = "";
    @Builder.Default
    private final List<String> options = Arrays.asList("", "", "", "");
    private final Integer correctOptionIndex;
    @Builder.Default
    private final String marksText = "";
}
