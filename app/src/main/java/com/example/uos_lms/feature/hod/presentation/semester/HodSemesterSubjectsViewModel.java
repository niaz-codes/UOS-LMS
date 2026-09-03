package com.example.uos_lms.feature.hod.presentation.semester;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodSemesterSubjectsViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final String departmentId;
    private final String semesterId;

    private final MutableLiveData<HodSemesterSubjectsUiState> uiState =
            new MutableLiveData<>(HodSemesterSubjectsUiState.initial());

    @Inject
    public HodSemesterSubjectsViewModel(
            SavedStateHandle savedStateHandle,
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource) {
        this.universityDataSource = universityDataSource;
        this.departmentId = savedStateHandle.get("departmentId");
        this.semesterId = savedStateHandle.get("semesterId");

        universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> {
            Semester semester = null;
            for (Semester candidate : semesters) {
                if (candidate.getId().equals(semesterId)) {
                    semester = candidate;
                    break;
                }
            }
            uiState.setValue(uiState.getValue().toBuilder().semester(semester).build());
        });

        universityDataSource.listSubjectsForSemester(semesterId).addOnSuccessListener(subjects -> {
            List<Subject> sorted = new ArrayList<>(subjects);
            sorted.sort((a, b) -> a.getCode().compareTo(b.getCode()));
            uiState.setValue(uiState.getValue().toBuilder().subjects(sorted).loading(false).build());
        });

        userDataSource.listApprovedTeachersInDepartment(departmentId).addOnSuccessListener(teachers ->
                uiState.setValue(uiState.getValue().toBuilder().teachers(teachers).build()));
    }

    public LiveData<HodSemesterSubjectsUiState> getUiState() {
        return uiState;
    }

    public void assignTeacher(String subjectId, User teacher) {
        universityDataSource.assignTeacher(subjectId, teacher.getUid())
                .addOnSuccessListener(this::replaceSubject)
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void unassignTeacher(String subjectId) {
        universityDataSource.unassignTeacher(subjectId)
                .addOnSuccessListener(this::replaceSubject)
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    /** REST is one-shot (no live listener to auto-refresh the row), so the updated Subject
     * returned by assign/unassign is spliced into the current list directly. */
    private void replaceSubject(Subject updated) {
        List<Subject> subjects = new ArrayList<>();
        for (Subject subject : uiState.getValue().getSubjects()) {
            subjects.add(subject.getId().equals(updated.getId()) ? updated : subject);
        }
        uiState.setValue(uiState.getValue().toBuilder().subjects(subjects).build());
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
