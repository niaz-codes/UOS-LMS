package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** One spreadsheet row in the Teacher's Exam Result screen: the student plus the joining
 * of their existing ExamResult (any workflow status, null if never entered) and, for the
 * repeat exam type, the latest RepeatExam for that result. */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ExamResultRosterRow {
    private final User student;
    private final ExamResult result;
    private final RepeatExam repeatExam;

    /** A row can only be (re)entered by the Teacher while its result has never been
     * submitted, or was bounced back by the HOD - matches the ExamResult edit rules. */
    public boolean isResultEditable() {
        return result == null
                || result.getStatus() == ResultStatus.DRAFT
                || result.getStatus() == ResultStatus.REJECTED;
    }
}
