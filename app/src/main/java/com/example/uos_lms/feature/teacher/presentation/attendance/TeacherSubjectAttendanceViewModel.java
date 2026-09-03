package com.example.uos_lms.feature.teacher.presentation.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.domain.model.AttendanceRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherSubjectAttendanceViewModel extends ViewModel {

    private final String subjectId;

    private final MutableLiveData<TeacherSubjectAttendanceUiState> uiState =
            new MutableLiveData<>(TeacherSubjectAttendanceUiState.initial());

    @Inject
    public TeacherSubjectAttendanceViewModel(SavedStateHandle savedStateHandle, ApiAttendanceDataSource attendanceDataSource) {
        this.subjectId = savedStateHandle.get("subjectId");
        attendanceDataSource.historyForSubject(subjectId)
                .addOnSuccessListener(records -> {
                    Set<String> dates = new LinkedHashSet<>();
                    for (AttendanceRecord record : records) {
                        dates.add(record.getDateKey());
                    }
                    List<String> sorted = new ArrayList<>(dates);
                    Collections.sort(sorted, Collections.reverseOrder());
                    uiState.setValue(uiState.getValue().toBuilder().historyDates(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).build()));
    }

    public LiveData<TeacherSubjectAttendanceUiState> getUiState() {
        return uiState;
    }
}
