package com.example.uos_lms.core.data.remote.api.dto;

import java.util.Collections;
import java.util.List;

/** UserResponseDto plus which of the teacher's own subjects this student belongs to (current
 * cohort of that subject's department+semester, or retaking it) - computed server-side in
 * subjectController.myStudents so the client never has to re-derive enrollment itself. */
public class TeacherStudentRowDto extends UserResponseDto {
    private List<String> subjectIds;

    public List<String> getSubjectIds() {
        return subjectIds != null ? subjectIds : Collections.emptyList();
    }
}
