package com.example.uos_lms.feature.teacher.presentation.repeatexam;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.domain.model.RepeatStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class RepeatExamRow {
    private final ExamResult examResult;
    @Nullable
    private final RepeatExam latestRepeat;
    private final String marksInput;

    /** Not editable only while a submitted attempt is awaiting HOD review - a fresh (never
     * attempted), rejected, or already-approved-but-still-failing result can always take a
     * new attempt. */
    public boolean isEditable() {
        return latestRepeat == null || latestRepeat.getRepeatStatus() != RepeatStatus.SUBMITTED;
    }

    /** A brand-new RepeatExam must be created before marks can be submitted, unless one is
     * already sitting in PENDING/REJECTED (editable in place). */
    public boolean needsFreshRepeat() {
        return latestRepeat == null || latestRepeat.getRepeatStatus() == RepeatStatus.APPROVED;
    }
}
