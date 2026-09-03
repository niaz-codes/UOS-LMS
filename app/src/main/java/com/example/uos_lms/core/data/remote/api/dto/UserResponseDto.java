package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserResponseDto {

    @SerializedName("_id")
    private String id;

    private String fullName;
    private String fatherName;
    private String cnic;
    private String phone;
    private String email;
    private String role;
    private String status;
    private String profilePhotoUrl;
    private String profilePhotoPublicId;
    private String departmentId;
    private List<String> departmentIds;
    private List<String> sessionIds;
    private List<String> semesterIds;
    private String sessionId;
    private String currentSemesterId;
    private String registrationNumber;
    private String rollNumber;
    private List<String> retakeSubjectIds;
    private String employeeId;
    private String designation;
    private String createdAt;

    public User toDomain() {
        return User.builder()
                .uid(id)
                .fullName(fullName)
                .fatherName(fatherName)
                .cnic(cnic)
                .phone(phone)
                .email(email)
                .role(UserRole.fromStringOrNull(role))
                .status(UserStatus.fromStringOrNull(status))
                .profilePhotoUrl(profilePhotoUrl)
                .profilePhotoPublicId(profilePhotoPublicId)
                .department(departmentId)
                .departmentIds(departmentIds != null ? departmentIds : Collections.emptyList())
                .semester(currentSemesterId)
                .employeeId(employeeId)
                .designation(designation)
                .registrationNumber(registrationNumber)
                .rollNumber(rollNumber)
                .sessionId(sessionId)
                .retakeSubjectIds(retakeSubjectIds != null ? retakeSubjectIds : Collections.emptyList())
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
