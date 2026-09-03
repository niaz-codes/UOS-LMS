package com.example.uos_lms.feature.auth.data;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** Immutable bag of the academic selections the applicant picked on the registration form.
 * Which fields are populated depends on the chosen role:
 * <ul>
 *     <li>STUDENT: {@link #departmentId} + {@link #sessionId} + {@link #semesterId}</li>
 *     <li>TEACHER: {@link #departmentIds} (multi-select, no session/semester)</li>
 *     <li>HOD: {@link #departmentId}</li>
 * </ul> */
public final class RegisterSelections {

    @Nullable
    private final String departmentId;
    @Nullable
    private final String sessionId;
    @Nullable
    private final String semesterId;
    private final List<String> departmentIds;

    public RegisterSelections(@Nullable String departmentId,
                              @Nullable String sessionId,
                              @Nullable String semesterId,
                              @Nullable List<String> departmentIds) {
        this.departmentId = departmentId;
        this.sessionId = sessionId;
        this.semesterId = semesterId;
        this.departmentIds = departmentIds != null ? departmentIds : Collections.emptyList();
    }

    public static RegisterSelections empty() {
        return new RegisterSelections(null, null, null, null);
    }

    @Nullable
    public String getDepartmentId() {
        return departmentId;
    }

    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    @Nullable
    public String getSemesterId() {
        return semesterId;
    }

    public List<String> getDepartmentIds() {
        return departmentIds;
    }
}
