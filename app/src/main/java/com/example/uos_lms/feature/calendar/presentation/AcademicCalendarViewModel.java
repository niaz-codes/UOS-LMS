package com.example.uos_lms.feature.calendar.presentation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiContentDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.CalendarEventType;
import com.example.uos_lms.core.domain.model.UserRole;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AcademicCalendarViewModel extends ViewModel {

    private final ApiContentDataSource contentDataSource;
    private final AuthApi authApi;

    private final MutableLiveData<AcademicCalendarUiState> uiState = new MutableLiveData<>(AcademicCalendarUiState.initial());

    @Inject
    public AcademicCalendarViewModel(ApiContentDataSource contentDataSource, AuthApi authApi) {
        this.contentDataSource = contentDataSource;
        this.authApi = authApi;
        load();
    }

    public LiveData<AcademicCalendarUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        RetrofitTasks.call(authApi.me()).addOnCompleteListener(meTask -> {
            boolean isAdmin = meTask.isSuccessful() && meTask.getResult() != null
                    && meTask.getResult().getUser().toDomain().getRole() == UserRole.ADMIN;
            uiState.setValue(uiState.getValue().toBuilder().admin(isAdmin).build());
        });
        contentDataSource.listCalendarEvents()
                .addOnSuccessListener(events -> uiState.setValue(uiState.getValue().toBuilder().events(events).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public void addEvent(String title, String description, CalendarEventType type, long dateMillis) {
        contentDataSource.createCalendarEvent(title, description, type.name(), dateMillis)
                .continueWithTask(created -> contentDataSource.listCalendarEvents())
                .addOnSuccessListener(events -> uiState.setValue(uiState.getValue().toBuilder().events(events).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void deleteEvent(String id) {
        contentDataSource.deleteCalendarEvent(id)
                .continueWithTask(deleted -> contentDataSource.listCalendarEvents())
                .addOnSuccessListener(events -> uiState.setValue(uiState.getValue().toBuilder().events(events).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
