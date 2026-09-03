package com.example.uos_lms.feature.teacher.presentation.attendance;

import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class RosterRow {
    private final User student;
    private final AttendanceStatus status;
}
