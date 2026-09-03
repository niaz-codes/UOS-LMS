package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import com.example.uos_lms.core.domain.model.Department;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** One expandable Department row in the Student Management tree - Department -> Session,
 * where selecting a Session navigates away instead of expanding further inline. */
@Getter
@Builder(toBuilder = true)
public class DepartmentStudentNode {
    private final Department department;
    private final boolean expanded;
    private final Integer totalCount;
    private final boolean loadingSessions;
    @Builder.Default
    private final String sessionSearchQuery = "";
    @Builder.Default
    private final List<SessionNode> sessions = Collections.emptyList();

    public static DepartmentStudentNode of(Department department) {
        return DepartmentStudentNode.builder().department(department).build();
    }

    public List<SessionNode> getFilteredSessions() {
        if (sessionSearchQuery.isBlank()) return sessions;
        String query = sessionSearchQuery.toLowerCase();
        List<SessionNode> result = new ArrayList<>();
        for (SessionNode node : sessions) {
            if (node.getSession().getLabel().toLowerCase().contains(query)) result.add(node);
        }
        return result;
    }
}
