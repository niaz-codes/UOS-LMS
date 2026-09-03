package com.example.uos_lms.feature.teacher.presentation.examresult.workspace;

import com.example.uos_lms.core.domain.model.ExamResultRosterRow;
import com.example.uos_lms.core.domain.model.GradeScale;
import com.example.uos_lms.core.domain.model.RepeatExam;

import lombok.Builder;
import lombok.Getter;

/** One spreadsheet row in the dedicated Exam Result screen: the roster row (student +
 * existing result + repeat exam) plus the teacher's typed marks. All derived display values
 * (grade/GPA/status) are computed locally from the marks via the same GradeScale the backend
 * uses, so the sheet updates live as the teacher types - the backend recomputes the
 * authoritative values on save/submit. */
@Getter
@Builder(toBuilder = true)
public class ResultEntryRow {
    private final ExamResultRosterRow source;
    @Builder.Default
    private final String marksInput = "";

    public boolean isEditable(String examType) {
        if (TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(examType)) {
            RepeatExam repeat = source.getRepeatExam();
            return repeat == null || repeat.isEditable();
        }
        return source.isResultEditable();
    }

    /** The 0-100 percentage shown/used for grading. Locked rows (already submitted/approved)
     * fall back to the stored result; editable rows scale the typed marks against the total. */
    public double displayPercentage(String examType, int total) {
        if (TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(examType)) {
            RepeatExam repeat = source.getRepeatExam();
            if (repeat != null && repeat.getNewMarks() != null && !isEditable(examType)) {
                return repeat.getNewMarks();
            }
            return percentageOf(marksInput, 100);
        }
        if (!isEditable(examType) && source.getResult() != null) {
            return source.getResult().getObtainedMarks();
        }
        return percentageOf(marksInput, total);
    }

    public boolean hasValidMarks(String examType, int total) {
        if (!isEditable(examType)) return true;
        Integer marks = parseIntOrNull(marksInput);
        if (marks == null) return false;
        int max = TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(examType) ? 100 : total;
        return marks >= 0 && marks <= max;
    }

    public String displayGrade(String examType, int total) {
        if (!hasValidMarks(examType, total)) return "—";
        return GradeScale.letterFor(displayPercentage(examType, total));
    }

    public double displayGpa(String examType, int total) {
        if (!hasValidMarks(examType, total)) return 0;
        return GradeScale.gpaPointFor(displayPercentage(examType, total));
    }

    public String displayStatus(String examType, int total) {
        String grade = displayGrade(examType, total);
        if ("—".equals(grade)) return "—";
        return "F".equals(grade) ? "FAIL" : "PASS";
    }

    /** Scaled to percentage the same way the rest of the app does when submitting. */
    public double percentageToSubmit(int total) {
        return percentageOf(marksInput, total);
    }

    public int repeatMarksToSubmit() {
        Integer marks = parseIntOrNull(marksInput);
        return marks != null ? marks : 0;
    }

    public static double percentageOf(String marksInput, int total) {
        if (total <= 0) return 0.0;
        Integer marks = parseIntOrNull(marksInput);
        if (marks == null) return 0.0;
        return (marks * 100.0) / total;
    }

    public static Integer parseIntOrNull(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
