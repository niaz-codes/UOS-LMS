package com.example.uos_lms.feature.admin.university.presentation.home;

import com.example.uos_lms.core.domain.model.UserRole;

import lombok.Builder;
import lombok.Getter;

/** One row in the dashboard's Pending Approvals hero list - just the fields that row needs to
 * render (name/role/department/registration date), derived from a pending User plus the
 * department id->name lookup, so the Fragment never has to reach back into raw User/Department
 * objects. */
@Getter
@Builder
public class PendingApprovalRow {
    private final String uid;
    private final String fullName;
    private final UserRole role;
    private final String departmentName;
    private final long createdAt;
}
