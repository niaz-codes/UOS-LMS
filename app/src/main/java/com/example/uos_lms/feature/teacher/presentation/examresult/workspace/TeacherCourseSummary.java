package com.example.uos_lms.feature.teacher.presentation.examresult.workspace;

import com.example.uos_lms.core.domain.model.Subject;

import lombok.Builder;
import lombok.Getter;

/** One "My Assigned Courses" card: a subject the teacher owns plus its roster/result stats,
 * built entirely from already-fetched data (no new backend endpoint). */
@Getter
@Builder
public class TeacherCourseSummary {
    private final Subject subject;
    private final String departmentName;
    private final String semesterLabel;
    private final String sessionLabel;
    private final int totalStudents;
    private final int resultsEntered;
    private final int resultsDraft;
    private final int resultsPending;
    private final int resultsApproved;
    private final int resultsRejected;

    public int getResultsPendingEntry() {
        return Math.max(0, totalStudents - resultsEntered);
    }

    public CourseResultStatus getStatus() {
        if (totalStudents == 0) return CourseResultStatus.NOT_STARTED;
        if (resultsEntered == 0) return CourseResultStatus.NOT_STARTED;
        if (resultsEntered < totalStudents) return CourseResultStatus.IN_PROGRESS;
        if (resultsApproved == resultsEntered) return CourseResultStatus.APPROVED;
        if (resultsPending > 0) return CourseResultStatus.SUBMITTED;
        return CourseResultStatus.COMPLETED;
    }
}
