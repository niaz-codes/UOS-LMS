package com.example.uos_lms.feature.admin.presentation.attendance;

import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.TeacherAttendanceRecord;
import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminTeacherAttendanceUiState {
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    private final Department selectedDepartment;
    @Builder.Default
    private final List<User> teachers = Collections.emptyList();
    private final User selectedTeacher;
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    private final Semester selectedSemester;
    @Builder.Default
    private final List<TeacherAttendanceRecord> records = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static AdminTeacherAttendanceUiState initial() {
        return AdminTeacherAttendanceUiState.builder().build();
    }

    public int getPresentCount() {
        int count = 0;
        for (TeacherAttendanceRecord record : records) {
            if (record.getStatus() == AttendanceStatus.PRESENT) count++;
        }
        return count;
    }

    public int getPercentage() {
        return records.isEmpty() ? 0 : (getPresentCount() * 100) / records.size();
    }
}
