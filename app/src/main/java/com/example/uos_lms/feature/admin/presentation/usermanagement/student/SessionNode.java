package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import com.example.uos_lms.core.domain.model.Session;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SessionNode {
    private final Session session;
    private final Integer totalCount;
}
