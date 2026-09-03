package com.example.uos_lms.feature.admin.presentation.usermanagement.common;

import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** One expandable Department row in the HOD/Teacher user-management tree, plus the search/
 * filter/sort/pagination state for its (lazily loaded) member list - Java port of the Compose
 * DepartmentHodNode, generalized to any role. */
@Getter
@Builder(toBuilder = true)
public class DepartmentUserNode {

    public static final int PAGE_SIZE = 20;

    private final Department department;
    private final boolean expanded;
    private final Integer totalCount;
    @Builder.Default
    private final List<User> users = Collections.emptyList();
    private final boolean loadingUsers;
    @Builder.Default
    private final String searchQuery = "";
    @Builder.Default
    private final UserFilter filter = UserFilter.ALL;
    @Builder.Default
    private final UserSortOption sortOption = UserSortOption.NAME_ASC;
    @Builder.Default
    private final int visibleCount = PAGE_SIZE;

    public static DepartmentUserNode of(Department department) {
        return DepartmentUserNode.builder().department(department).build();
    }

    public List<User> getFilteredUsers() {
        List<User> result = new ArrayList<>();
        String query = searchQuery.toLowerCase();
        for (User user : users) {
            if (filter.getStatus() != null && user.getStatus() != filter.getStatus()) continue;
            if (!query.isEmpty()
                    && !user.getFullName().toLowerCase().contains(query)
                    && !user.getEmail().toLowerCase().contains(query)) {
                continue;
            }
            result.add(user);
        }
        UserSortHelper.sort(result, sortOption);
        return result;
    }

    public List<User> getVisibleUsers() {
        List<User> filtered = getFilteredUsers();
        return filtered.subList(0, Math.min(visibleCount, filtered.size()));
    }

    public boolean isHasMore() {
        return getFilteredUsers().size() > visibleCount;
    }
}
