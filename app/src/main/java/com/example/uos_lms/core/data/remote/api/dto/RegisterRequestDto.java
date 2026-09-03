package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterRequestDto {
    private final String fullName;
    private final String fatherName;
    private final String cnic;
    private final String phone;
    private final String email;
    private final String password;
    private final String role;
    private final String registrationNumber;
    private final String rollNumber;
    private final String employeeId;
    private final String designation;

    // Single-select academic placement: STUDENT (department + session + current semester)
    // and HOD (department). Sent only when the applicant picked them on the registration form.
    private final String departmentId;
    private final String sessionId;
    private final String currentSemesterId;

    // Multi-select academic placement: TEACHER covers several departments (no session/semester).
    private final List<String> departmentIds;
}
