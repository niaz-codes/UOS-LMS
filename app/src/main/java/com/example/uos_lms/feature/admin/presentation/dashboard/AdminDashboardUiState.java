package com.example.uos_lms.feature.admin.presentation.dashboard;

import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminDashboardUiState {
    @Builder.Default
    private final List<User> allUsers = Collections.emptyList();
    @Builder.Default
    private final String searchQuery = "";
    @Builder.Default
    private final UserFilter selectedFilter = UserFilter.ALL;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;
    private final String actionMessage;

    public static AdminDashboardUiState initial() {
        return AdminDashboardUiState.builder().build();
    }

    public List<User> getFilteredUsers() {
        List<User> result = new ArrayList<>();
        String query = searchQuery.toLowerCase();
        for (User user : allUsers) {
            if (selectedFilter.getStatus() != null && user.getStatus() != selectedFilter.getStatus()) continue;
            if (!query.isEmpty()
                    && !user.getFullName().toLowerCase().contains(query)
                    && !user.getEmail().toLowerCase().contains(query)
                    && !user.getCnic().contains(searchQuery)) {
                continue;
            }
            result.add(user);
        }
        return result;
    }
}
