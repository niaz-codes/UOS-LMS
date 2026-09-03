package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class User {
    private final String uid;
    private final String fullName;
    private final String fatherName;
    private final String cnic;
    private final String phone;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final String profilePhotoUrl;
    private final String profilePhotoPublicId;

    // HOD/STUDENT: single department, `department` is authoritative.
    // TEACHER: multi-department, `departmentIds` is authoritative and `department`
    // mirrors departmentIds.get(0) for legacy single-department queries/display.
    private final String department;
    @Builder.Default
    private final List<String> departmentIds = Collections.emptyList();
    private final String semester;
    private final String employeeId;
    private final String designation;
    private final String registrationNumber;
    private final String rollNumber;
    private final String sessionId;
    // STUDENT only: subject ids the student failed in a prior semester and must
    // retake alongside their current semester's subjects (see promoteStudents()).
    @Builder.Default
    private final List<String> retakeSubjectIds = Collections.emptyList();
    @Builder.Default
    private final long createdAt = 0L;
}
