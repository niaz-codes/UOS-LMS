package com.example.uos_lms.feature.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiNotificationDataSource;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.example.uos_lms.core.domain.model.NotificationSettings;

import java.util.function.Function;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NotificationSettingsViewModel extends ViewModel {

    private final ApiNotificationDataSource notificationDataSource;

    private final MutableLiveData<NotificationSettingsUiState> uiState = new MutableLiveData<>(NotificationSettingsUiState.initial());

    @Inject
    public NotificationSettingsViewModel(ApiNotificationDataSource notificationDataSource) {
        this.notificationDataSource = notificationDataSource;
        load();
    }

    public LiveData<NotificationSettingsUiState> getUiState() {
        return uiState;
    }

    private void load() {
        notificationDataSource.getSettings()
                .addOnSuccessListener(settings -> uiState.setValue(uiState.getValue().toBuilder().settings(settings).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public void setPushEnabled(boolean value) {
        applyUpdate(s -> s.toBuilder().pushEnabled(value).build());
    }

    public void setSoundEnabled(boolean value) {
        applyUpdate(s -> s.toBuilder().soundEnabled(value).build());
    }

    public void setVibrationEnabled(boolean value) {
        applyUpdate(s -> s.toBuilder().vibrationEnabled(value).build());
    }

    public void setCategoryEnabled(NotificationCategory category, boolean value) {
        applyUpdate(s -> applyCategory(s, category, value));
    }

    private NotificationSettings applyCategory(NotificationSettings settings, NotificationCategory category, boolean value) {
        NotificationSettings.NotificationSettingsBuilder builder = settings.toBuilder();
        switch (category) {
            case ACCOUNT:
                return builder.account(value).build();
            case ACADEMIC_CONTENT:
                return builder.academicContent(value).build();
            case ATTENDANCE:
                return builder.attendance(value).build();
            case EXAM_RESULT:
                return builder.examResult(value).build();
            case LEAVE:
                return builder.leave(value).build();
            case ANNOUNCEMENT:
                return builder.announcement(value).build();
            case CALENDAR:
                return builder.calendar(value).build();
            case MESSAGE:
                return builder.message(value).build();
            case PROMOTION:
                return builder.promotion(value).build();
            case SYSTEM:
            default:
                return builder.system(value).build();
        }
    }

    private void applyUpdate(Function<NotificationSettings, NotificationSettings> mutator) {
        NotificationSettingsUiState current = uiState.getValue();
        NotificationSettings updated = mutator.apply(current.getSettings());
        uiState.setValue(current.toBuilder().settings(updated).saving(true).errorMessage(null).build());
        notificationDataSource.updateSettings(updated)
                .addOnSuccessListener(saved -> uiState.setValue(uiState.getValue().toBuilder().settings(saved).saving(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().saving(false).errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
