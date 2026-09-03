package com.example.uos_lms.core.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** One department node of the teacher's Exam Result workspace, carrying its sessions
 * (cohorts with actual students in the teacher's subjects) and semesters (curriculum
 * semesters the teacher teaches there). */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DepartmentOptions {
    private final Department department;
    @Builder.Default
    private final List<Session> sessions = Collections.emptyList();
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();

    public List<String> getSessionIds() {
        List<String> ids = new ArrayList<>();
        for (Session session : sessions) {
            ids.add(session.getId());
        }
        return ids;
    }

    public List<String> getSemesterIds() {
        List<String> ids = new ArrayList<>();
        for (Semester semester : semesters) {
            ids.add(semester.getId());
        }
        return ids;
    }

    public static DepartmentOptions findById(List<DepartmentOptions> options, String departmentId) {
        if (options == null || departmentId == null) return null;
        for (DepartmentOptions option : options) {
            if (option.getDepartment().getId().equals(departmentId)) return option;
        }
        return null;
    }
}
