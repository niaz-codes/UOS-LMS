package com.example.uos_lms.feature.teacher.presentation.students;

import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** One deduplicated student row on the Teacher's Students screen: the student plus which of the
 * teacher's own assigned subjects they belong to (resolved from TeacherStudentRoster) and the
 * display names for their department/session/semester. */
@Getter
@Builder
public class TeacherStudentSummary {
    private final User user;
    @Builder.Default
    private final List<String> subjectIds = Collections.emptyList();
    @Builder.Default
    private final List<Subject> courses = Collections.emptyList();
    private final String departmentName;
    private final String sessionLabel;
    private final String semesterLabel;
}
