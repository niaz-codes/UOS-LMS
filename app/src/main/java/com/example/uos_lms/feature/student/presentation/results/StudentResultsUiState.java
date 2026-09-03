package com.example.uos_lms.feature.student.presentation.results;

import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentResultsUiState {
    @Builder.Default
    private final double cgpa = 0.0;
    @Builder.Default
    private final int totalCreditHours = 0;
    @Builder.Default
    private final String sessionLabel = "";
    @Builder.Default
    private final int semesterCount = 0;
    @Builder.Default
    private final boolean hasResults = false;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;
    @Builder.Default
    private final List<StudentSemesterResultSummary> results = Collections.emptyList();

    public static StudentResultsUiState initial() {
        return StudentResultsUiState.builder().build();
    }

    /** Semesters in natural reading order (1, 2, 3...) for the grid - the backend returns them
     * most-recently-updated-first, which reads oddly as a "my semesters" grid. */
    public List<StudentSemesterResultSummary> getSortedResults() {
        List<StudentSemesterResultSummary> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparingInt(StudentSemesterResultSummary::getSemesterNumber));
        return sorted;
    }

    public double getBestGpa() {
        double best = 0.0;
        for (StudentSemesterResultSummary result : results) {
            if (result.getSemesterGpa() > best) best = result.getSemesterGpa();
        }
        return best;
    }

    public int getTotalSubjects() {
        int total = 0;
        for (StudentSemesterResultSummary result : results) total += result.getTotalSubjectCount();
        return total;
    }
}
