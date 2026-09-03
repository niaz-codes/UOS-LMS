package com.example.uos_lms.core.data.remote.api.dto;

import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** GET /api/grading/exam-result/roster - one spreadsheet row per student for the selected
 * subject + exam type. `result` is the student's existing ExamResult (null if none yet),
 * `repeatExam` the latest repeat exam for that result (repeat type only). */
@Data
@NoArgsConstructor
public class ExamResultRosterEnvelopeDto {
    private List<ExamResultRosterRowDto> rows;

    public List<ExamResultRosterRowDto> getRows() {
        return rows != null ? rows : Collections.emptyList();
    }
}
