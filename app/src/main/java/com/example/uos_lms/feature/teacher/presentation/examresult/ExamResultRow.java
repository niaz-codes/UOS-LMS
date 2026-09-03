package com.example.uos_lms.feature.teacher.presentation.examresult;

import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.domain.model.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ExamResultRow {
    private final User student;
    private final ExamResult existing;
    private final String marksInput;

    /** A row can only be (re)entered by the Teacher while it has never been submitted,
     * or was bounced back by the HOD - matches firestore.rules' update condition exactly. */
    public boolean isEditable() {
        return existing == null || existing.getStatus() == ResultStatus.DRAFT || existing.getStatus() == ResultStatus.REJECTED;
    }
}
