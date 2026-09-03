package com.example.uos_lms.feature.hod.presentation.timetable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiSchedulingDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.google.android.gms.tasks.Tasks;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodTimetableViewModel extends ViewModel {

    private final MutableLiveData<HodTimetableUiState> uiState = new MutableLiveData<>(HodTimetableUiState.initial());

    @Inject
    public HodTimetableViewModel(ApiSchedulingDataSource schedulingDataSource, AuthApi authApi) {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    String departmentId = user != null ? user.getDepartment() : null;
                    if (departmentId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
                        return;
                    }
                    schedulingDataSource.slotsForDepartment(departmentId)
                            .addOnSuccessListener(slots -> uiState.setValue(uiState.getValue().toBuilder().allSlots(slots).loading(false).build()))
                            .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<HodTimetableUiState> getUiState() {
        return uiState;
    }

    public void selectDay(DayOfWeek day) {
        uiState.setValue(uiState.getValue().toBuilder().selectedDay(day).build());
    }
}
