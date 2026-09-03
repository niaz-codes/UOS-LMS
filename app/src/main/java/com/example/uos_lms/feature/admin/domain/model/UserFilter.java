package com.example.uos_lms.feature.admin.domain.model;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.domain.model.UserStatus;

public enum UserFilter {
    ALL("All", null),
    PENDING("Pending", UserStatus.PENDING),
    APPROVED("Approved", UserStatus.APPROVED),
    REJECTED("Rejected", UserStatus.REJECTED),
    SUSPENDED("Suspended", UserStatus.SUSPENDED);

    private final String label;
    @Nullable
    private final UserStatus status;

    UserFilter(String label, @Nullable UserStatus status) {
        this.label = label;
        this.status = status;
    }

    public String getLabel() {
        return label;
    }

    @Nullable
    public UserStatus getStatus() {
        return status;
    }
}
