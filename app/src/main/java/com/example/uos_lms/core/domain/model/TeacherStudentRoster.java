package com.example.uos_lms.core.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** The authoritative "my students" union for the currently logged-in Teacher - every assigned
 * subject's roster size, and the deduplicated student list with which of those subjects each
 * student actually belongs to. Built server-side (subjectController.myStudents) from the
 * teacher's own identity only, so this is already exactly what the Teacher is authorized to
 * see - no further client-side filtering by department/semester should ever widen it. */
@Getter
@Builder
public class TeacherStudentRoster {
    private final List<SubjectRoster> subjects;
    private final List<StudentEnrollment> students;

    @Getter
    @Builder
    public static class SubjectRoster {
        private final Subject subject;
        private final int studentCount;
    }

    @Getter
    @Builder
    public static class StudentEnrollment {
        private final User user;
        private final List<String> subjectIds;
    }
}
