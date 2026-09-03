package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.QuizAnswer;
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuizAttemptResponseDto {
    @SerializedName("_id")
    private String id;
    private String quizId;
    private String subjectId;
    private PersonRefDto studentId;
    private List<QuizAnswerDto> answers;
    private int score;
    private int totalMarks;
    private String startedAt;
    private String submittedAt;
    private Integer manualScore;
    private String feedback;
    private String gradedBy;
    private String gradedAt;

    public QuizAttempt toDomain() {
        List<QuizAnswer> mappedAnswers = new ArrayList<>();
        if (answers != null) {
            for (QuizAnswerDto dto : answers) mappedAnswers.add(dto.toDomain());
        }
        return QuizAttempt.builder()
                .id(id)
                .quizId(quizId)
                .subjectId(subjectId)
                .studentUid(studentId != null ? studentId.getId() : null)
                .studentName(studentId != null ? studentId.getFullName() : null)
                .answers(mappedAnswers)
                .score(score)
                .totalMarks(totalMarks)
                .startedAt(startedAt != null ? IsoDates.toMillis(startedAt) : 0L)
                .submittedAt(submittedAt != null ? IsoDates.toMillis(submittedAt) : 0L)
                .manualScore(manualScore)
                .feedback(feedback)
                .gradedBy(gradedBy)
                .gradedAt(gradedAt != null ? IsoDates.toMillis(gradedAt) : null)
                .build();
    }
}
