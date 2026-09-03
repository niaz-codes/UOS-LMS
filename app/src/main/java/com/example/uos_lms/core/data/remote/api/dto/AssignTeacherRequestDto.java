package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

/** unassign is a plain boolean flag rather than relying on the client sending a literal
 * JSON null for teacherId - Gson omits null fields on serialize by default, which would
 * otherwise silently no-op, see subjectController.assignTeacher. */
@Data
@Builder
public class AssignTeacherRequestDto {
    private final String teacherId;
    private final boolean unassign;
}
