package com.example.uos_lms.feature.teacher.presentation.students;

import com.example.uos_lms.core.domain.model.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherStudentsUiState {
    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    @Builder.Default
    private final List<TeacherStudentSummary> allStudents = Collections.emptyList();
    @Builder.Default
    private final String searchQuery = "";
    private final Subject selectedSubject;

    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static TeacherStudentsUiState initial() {
        return TeacherStudentsUiState.builder().build();
    }

    /** Search + the "one of my subjects" filter, applied client-side over the already-loaded,
     * already-authorized roster - never a re-query with a wider scope. */
    public List<TeacherStudentSummary> getFilteredStudents() {
        List<TeacherStudentSummary> result = new ArrayList<>();
        String query = searchQuery.toLowerCase();
        for (TeacherStudentSummary summary : allStudents) {
            if (selectedSubject != null && !summary.getSubjectIds().contains(selectedSubject.getId())) {
                continue;
            }
            if (!query.isEmpty()
                    && !summary.getUser().getFullName().toLowerCase().contains(query)
                    && !matches(summary.getUser().getRollNumber(), query)
                    && !matches(summary.getUser().getRegistrationNumber(), query)
                    && !matches(summary.getUser().getEmail(), query)) {
                continue;
            }
            result.add(summary);
        }
        result.sort((a, b) -> a.getUser().getFullName().compareToIgnoreCase(b.getUser().getFullName()));
        return result;
    }

    private static boolean matches(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
