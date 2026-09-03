package com.example.uos_lms.feature.admin.presentation.promotion;

import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminPromotionUiState {
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    private final String selectedDepartmentId;
    private final String selectedDepartmentName;

    @Builder.Default
    private final List<Session> sessions = Collections.emptyList();
    private final String selectedSessionId;
    private final String selectedSessionLabel;

    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    private final String selectedSemesterId;
    private final int currentSemesterNumber;
    /** The department's Semester with number == currentSemesterNumber + 1, or null if it
     * hasn't been created yet - promotion is blocked until it exists. */
    private final Semester targetSemester;

    @Builder.Default
    private final boolean loadingRoster = false;
    @Builder.Default
    private final boolean refreshing = false;
    @Builder.Default
    private final List<PromotionStudentRow> rows = Collections.emptyList();

    @Builder.Default
    private final boolean promoting = false;
    private final String errorMessage;
    private final String actionMessage;

    public boolean isSemesterChosen() {
        return selectedDepartmentId != null && selectedSessionId != null && selectedSemesterId != null;
    }

    public int getSelectedCount() {
        int count = 0;
        for (PromotionStudentRow row : rows) {
            if (row.isSelected() && !row.isAlreadyPromoted()) count++;
        }
        return count;
    }

    public boolean canPromote() {
        return targetSemester != null && !promoting && getSelectedCount() > 0;
    }

    public static AdminPromotionUiState initial() {
        return AdminPromotionUiState.builder().build();
    }
}
