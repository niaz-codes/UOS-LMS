package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;
import com.example.uos_lms.feature.admin.presentation.usermanagement.common.UserSortHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SemesterStudentsNode {

    public static final int PAGE_SIZE = 20;

    private final Semester semester;
    private final boolean expanded;
    private final Integer totalCount;
    @Builder.Default
    private final List<User> students = Collections.emptyList();
    private final boolean loadingStudents;
    @Builder.Default
    private final String searchQuery = "";
    @Builder.Default
    private final UserFilter filter = UserFilter.ALL;
    @Builder.Default
    private final UserSortOption sortOption = UserSortOption.NAME_ASC;
    @Builder.Default
    private final int visibleCount = PAGE_SIZE;

    public static SemesterStudentsNode of(Semester semester) {
        return SemesterStudentsNode.builder().semester(semester).build();
    }

    public List<User> getFilteredStudents() {
        List<User> result = new ArrayList<>();
        String query = searchQuery.toLowerCase();
        for (User user : students) {
            if (filter.getStatus() != null && user.getStatus() != filter.getStatus()) continue;
            if (!query.isEmpty()
                    && !user.getFullName().toLowerCase().contains(query)
                    && !user.getEmail().toLowerCase().contains(query)
                    && !(user.getRegistrationNumber() != null && user.getRegistrationNumber().toLowerCase().contains(query))
                    && !(user.getRollNumber() != null && user.getRollNumber().toLowerCase().contains(query))) {
                continue;
            }
            result.add(user);
        }
        UserSortHelper.sort(result, sortOption);
        return result;
    }

    public List<User> getVisibleStudents() {
        List<User> filtered = getFilteredStudents();
        return filtered.subList(0, Math.min(visibleCount, filtered.size()));
    }

    public boolean isHasMore() {
        return getFilteredStudents().size() > visibleCount;
    }
}
