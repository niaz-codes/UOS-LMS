package com.example.uos_lms.feature.teacher.presentation.attendance;

import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.TeacherAttendanceRecord;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherOwnAttendanceUiState {
    @Builder.Default
    private final List<TeacherAttendanceRecord> records = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static TeacherOwnAttendanceUiState initial() {
        return TeacherOwnAttendanceUiState.builder().build();
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
