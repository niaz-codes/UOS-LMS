package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/** Exactly one of departmentId/departmentIds is set, depending on the target user's role
 * (HOD/Student get a single departmentId, Teacher gets the departmentIds array) - see
 * userController.updateDepartment. unassign is a plain boolean flag rather than relying on
 * a literal JSON null for departmentId (Gson omits null fields on serialize by default). */
@Data
@Builder
public class UpdateUserDepartmentRequestDto {
    private final String departmentId;
    private final List<String> departmentIds;
    private final boolean unassign;
}
