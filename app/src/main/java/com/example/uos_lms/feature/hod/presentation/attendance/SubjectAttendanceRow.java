package com.example.uos_lms.feature.hod.presentation.attendance;

import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Subject;

import lombok.Builder;
import lombok.Getter;

/** One course's teacher-attendance row for the mark screen - a teacher can have a different
 * status per course on the same day, so this is keyed by subject, not by teacher. */
@Getter
@Builder(toBuilder = true)
public class SubjectAttendanceRow {
    private final Subject subject;
    private final AttendanceStatus status;
}
