package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Course-wise teacher attendance: was this subject's assigned teacher present for THIS course
 * on a given day - a teacher can be PRESENT for one course and ABSENT for another on the same
 * date, since each record is scoped to one (subject, dateKey) session. Distinct from
 * AttendanceRecord, which is a student's attendance in a subject's class session. */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class TeacherAttendanceRecord {
    private final String id;
    private final String departmentId;
    private final String semesterId;
    private final String subjectId;
    private final String subjectCode;
    private final String subjectTitle;
    private final String dateKey;
    private final long dateMillis;
    private final String teacherUid;
    private final String teacherName;
    private final AttendanceStatus status;
    private final String markedBy;
    @Builder.Default
    private final long createdAt = 0L;
}
