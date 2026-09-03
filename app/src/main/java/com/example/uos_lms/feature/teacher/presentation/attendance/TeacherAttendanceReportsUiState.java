package com.example.uos_lms.feature.teacher.presentation.attendance;

import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Subject;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherAttendanceReportsUiState {
    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    private final Subject selectedSubject;
    @Builder.Default
    private final List<AttendanceRecord> records = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static TeacherAttendanceReportsUiState initial() {
        return TeacherAttendanceReportsUiState.builder().build();
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
