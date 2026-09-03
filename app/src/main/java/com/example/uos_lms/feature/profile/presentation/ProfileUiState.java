package com.example.uos_lms.feature.profile.presentation;

import com.example.uos_lms.core.domain.model.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ProfileUiState {
    private final User user;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    @Builder.Default
    private final boolean uploadingPhoto = false;
    private final Integer uploadProgress;
    private final String errorMessage;

    // Role context, resolved from the user's raw foreign-key fields so the Profile screen can
    // display names instead of ids - all nullable/blank until resolved, and simply omitted from
    // display if never resolved (a role that doesn't have that concept, e.g. Admin has no
    // department).
    private final String departmentName;
    private final String sessionLabel;
    private final String semesterLabel;
    /** Subjects the user teaches (Teacher) or is enrolled in (Student), null for roles where the
     * concept doesn't apply (Admin/HOD). */
    private final Integer subjectsCount;

    public static ProfileUiState initial() {
        return ProfileUiState.builder().build();
    }
}
