package com.example.uos_lms.core.domain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum UserRole {
    ADMIN, HOD, TEACHER, STUDENT;

    public static final List<UserRole> REGISTERABLE_ROLES =
            Collections.unmodifiableList(Arrays.asList(HOD, TEACHER, STUDENT));
    public static final List<UserRole> LOGIN_ROLES =
            Collections.unmodifiableList(Arrays.asList(ADMIN, HOD, TEACHER, STUDENT));

    public static UserRole fromStringOrNull(String raw) {
        if (raw == null) return null;
        for (UserRole role : values()) {
            if (role.name().equals(raw)) return role;
        }
        return null;
    }
}
