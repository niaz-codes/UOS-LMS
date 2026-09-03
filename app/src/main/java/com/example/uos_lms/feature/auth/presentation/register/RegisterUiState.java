package com.example.uos_lms.feature.auth.presentation.register;

import android.net.Uri;

import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.UserRole;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class RegisterUiState {
    @Builder.Default
    private final String fullName = "";
    @Builder.Default
    private final String fatherName = "";
    @Builder.Default
    private final String cnic = "";
    @Builder.Default
    private final String phone = "";
    @Builder.Default
    private final String email = "";
    @Builder.Default
    private final String password = "";
    @Builder.Default
    private final String confirmPassword = "";
    @Builder.Default
    private final UserRole role = UserRole.STUDENT;

    /** Cropped photo picked by the user, staged locally until submit() uploads it alongside
     * the rest of the form - null means no photo was picked (upload is entirely optional). */
    private final Uri photoUri;

    private final String fullNameError;
    private final String fatherNameError;
    private final String cnicError;
    private final String phoneError;
    private final String emailError;
    private final String passwordError;
    private final String confirmPasswordError;

    // ---- Department / Session / Semester options (loaded from the public backend API) ----
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    @Builder.Default
    private final boolean departmentsLoading = false;
    private final String departmentsError;

    /** Options for the currently selected STUDENT department (loaded after it is picked). */
    @Builder.Default
    private final List<Session> studentSessions = Collections.emptyList();
    @Builder.Default
    private final List<Semester> studentSemesters = Collections.emptyList();

    /** True while sessions/semesters are being fetched for the current selection. */
    @Builder.Default
    private final boolean selectionLoading = false;
    private final String selectionError;

    // ---- Selections (kept separately per role so switching roles never loses them) ----
    private final String studentDepartmentId;
    private final String studentSessionId;
    private final String studentSemesterId;

    @Builder.Default
    private final List<String> teacherDepartmentIds = Collections.emptyList();

    private final String hodDepartmentId;

    // ---- Per-section validation errors ----
    private final String departmentError;
    private final String sessionError;
    private final String semesterError;

    private final boolean loading;
    private final String submitError;

    public static RegisterUiState initial() {
        return RegisterUiState.builder().build();
    }
}
