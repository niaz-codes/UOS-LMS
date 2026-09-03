package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.domain.model.NotificationSettings;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors backend/src/models/User.js's notificationSettings sub-document field-for-field -
 * category keys are the backend's UPPER_SNAKE NotificationCategory values. */
@Data
@NoArgsConstructor
public class NotificationSettingsDto {
    private Boolean pushEnabled;
    private Boolean soundEnabled;
    private Boolean vibrationEnabled;

    @SerializedName("ACCOUNT")
    private Boolean account;
    @SerializedName("ACADEMIC_CONTENT")
    private Boolean academicContent;
    @SerializedName("ATTENDANCE")
    private Boolean attendance;
    @SerializedName("EXAM_RESULT")
    private Boolean examResult;
    @SerializedName("LEAVE")
    private Boolean leave;
    @SerializedName("ANNOUNCEMENT")
    private Boolean announcement;
    @SerializedName("CALENDAR")
    private Boolean calendar;
    @SerializedName("MESSAGE")
    private Boolean message;
    @SerializedName("PROMOTION")
    private Boolean promotion;
    @SerializedName("SYSTEM")
    private Boolean system;

    public NotificationSettings toDomain() {
        return NotificationSettings.builder()
                .pushEnabled(orTrue(pushEnabled))
                .soundEnabled(orTrue(soundEnabled))
                .vibrationEnabled(orTrue(vibrationEnabled))
                .account(orTrue(account))
                .academicContent(orTrue(academicContent))
                .attendance(orTrue(attendance))
                .examResult(orTrue(examResult))
                .leave(orTrue(leave))
                .announcement(orTrue(announcement))
                .calendar(orTrue(calendar))
                .message(orTrue(message))
                .promotion(orTrue(promotion))
                .system(orTrue(system))
                .build();
    }

    private static boolean orTrue(Boolean value) {
        return value == null || value;
    }

    public static NotificationSettingsDto fromDomain(NotificationSettings settings) {
        NotificationSettingsDto dto = new NotificationSettingsDto();
        dto.pushEnabled = settings.isPushEnabled();
        dto.soundEnabled = settings.isSoundEnabled();
        dto.vibrationEnabled = settings.isVibrationEnabled();
        dto.account = settings.isAccount();
        dto.academicContent = settings.isAcademicContent();
        dto.attendance = settings.isAttendance();
        dto.examResult = settings.isExamResult();
        dto.leave = settings.isLeave();
        dto.announcement = settings.isAnnouncement();
        dto.calendar = settings.isCalendar();
        dto.message = settings.isMessage();
        dto.promotion = settings.isPromotion();
        dto.system = settings.isSystem();
        return dto;
    }
}
