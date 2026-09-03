package com.example.uos_lms.core.session;

import com.example.uos_lms.core.domain.model.UserRole;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CachedSession {
    private final String uid;
    private final UserRole role;
    private final String fullName;
}
