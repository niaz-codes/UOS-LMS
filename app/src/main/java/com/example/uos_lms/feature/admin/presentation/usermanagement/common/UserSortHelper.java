package com.example.uos_lms.feature.admin.presentation.usermanagement.common;

import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;

import java.util.Comparator;
import java.util.List;

public final class UserSortHelper {

    private UserSortHelper() {
    }

    public static void sort(List<User> users, UserSortOption option) {
        Comparator<User> comparator;
        if (option == UserSortOption.NAME_DESC) {
            comparator = Comparator.comparing((User u) -> u.getFullName().toLowerCase()).reversed();
        } else if (option == UserSortOption.STATUS) {
            comparator = Comparator.comparing(u -> u.getStatus().name());
        } else if (option == UserSortOption.NEWEST) {
            comparator = Comparator.comparingLong(User::getCreatedAt).reversed();
        } else {
            comparator = Comparator.comparing((User u) -> u.getFullName().toLowerCase());
        }
        users.sort(comparator);
    }
}
