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
public class QuizAttempt {
    private final String id;
    private final String quizId;
    private final String subjectId;
    private final String studentUid;
    private final String studentName;
    @Builder.Default
    private final List<QuizAnswer> answers = Collections.emptyList();
    private final int score;
    private final int totalMarks;
    @Builder.Default
    private final long startedAt = 0L;
    @Builder.Default
    private final long submittedAt = 0L;
    private final Integer manualScore;
    private final String feedback;
    private final String gradedBy;
    private final Long gradedAt;

    public int getEffectiveScore() {
        return manualScore != null ? manualScore : score;
    }

    public boolean isManuallyGraded() {
        return manualScore != null;
    }

    public boolean isSubmitted() {
        return submittedAt != 0L;
    }
}
