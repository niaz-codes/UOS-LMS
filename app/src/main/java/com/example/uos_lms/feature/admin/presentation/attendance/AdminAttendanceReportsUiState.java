package com.example.uos_lms.feature.admin.presentation.attendance;

import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminAttendanceReportsUiState {
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    private final Department selectedDepartment;
    private final Semester selectedSemester;
    @Builder.Default
    private final List<AttendanceRecord> records = Collections.emptyList();
    private final boolean loading;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static AdminAttendanceReportsUiState initial() {
        return AdminAttendanceReportsUiState.builder().build();
    }

    public int getPresentCount() {
        int count = 0;
        for (AttendanceRecord record : records) {
            if (record.getStatus() == AttendanceStatus.PRESENT) count++;
        }
        return count;
    }

    public int getPercentage() {
        return records.isEmpty() ? 0 : (getPresentCount() * 100) / records.size();
    }
}
