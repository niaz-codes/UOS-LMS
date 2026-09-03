package com.example.uos_lms.feature.teacher.presentation.examresult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherExamResultUiState {
    @Builder.Default
    private final String totalMarksInput = "100";
    @Builder.Default
    private final List<ExamResultRow> rows = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean saving = false;
    private final String errorMessage;
    private final String actionMessage;

    public List<ExamResultRow> getEditableRows() {
        List<ExamResultRow> editable = new ArrayList<>();
        for (ExamResultRow row : rows) {
            if (row.isEditable()) editable.add(row);
        }
        return editable;
    }

    /** Once any row has left DRAFT, total marks is locked so the whole class stays on one scale. */
    public boolean isTotalMarksLocked() {
        for (ExamResultRow row : rows) {
            if (!row.isEditable()) return true;
        }
        return false;
    }

    public boolean canSubmit() {
        Integer totalMarks = parseIntOrNull(totalMarksInput);
        if (totalMarks == null || totalMarks <= 0) return false;
        List<ExamResultRow> editable = getEditableRows();
        if (editable.isEmpty()) return false;
        for (ExamResultRow row : editable) {
            Integer marks = parseIntOrNull(row.getMarksInput());
            if (marks == null || marks < 0 || marks > totalMarks) return false;
        }
        return true;
    }

    static Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static TeacherExamResultUiState initial() {
        return TeacherExamResultUiState.builder().build();
    }
}
