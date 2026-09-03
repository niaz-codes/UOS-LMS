package com.example.uos_lms.feature.admin.presentation.assignments;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Subject;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminAssignmentMonitorViewModel extends ViewModel {

    private final ApiAssignmentDataSource assignmentDataSource;

    private final MutableLiveData<AdminAssignmentMonitorUiState> uiState =
            new MutableLiveData<>(AdminAssignmentMonitorUiState.initial());

    private final ApiUniversityDataSource universityDataSource;

    @Inject
    public AdminAssignmentMonitorViewModel(
            ApiAssignmentDataSource assignmentDataSource,
            ApiUniversityDataSource universityDataSource) {
        this.assignmentDataSource = assignmentDataSource;
        this.universityDataSource = universityDataSource;
        load(universityDataSource);
    }

    private void load(ApiUniversityDataSource universityDataSource) {
        universityDataSource.listDepartments().onSuccessTask(departments -> {
            List<Task<List<Subject>>> subjectTasks = new ArrayList<>();
            for (Department department : departments) {
                subjectTasks.add(universityDataSource.listSubjectsForDepartment(department.getId()));
            }
            return Tasks.whenAllSuccess(subjectTasks);
        }).addOnSuccessListener(results -> {
            List<Subject> allSubjects = new ArrayList<>();
            for (Object result : results) {
                //noinspection unchecked
                allSubjects.addAll((List<Subject>) result);
            }
            Map<String, Subject> byId = new HashMap<>();
            for (Subject subject : allSubjects) byId.put(subject.getId(), subject);

            assignmentDataSource.allAssignments().addOnSuccessListener(assignments -> {
                List<Assignment> sorted = new ArrayList<>(assignments);
                sorted.sort(Comparator.comparingLong(Assignment::getDueDateMillis).reversed());
                uiState.setValue(uiState.getValue().toBuilder()
                        .assignments(sorted)
                        .subjectsById(byId)
                        .loading(false)
                        .build());
            }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<AdminAssignmentMonitorUiState> getUiState() {
        return uiState;
    }

    public void deleteAssignment(String id) {
        assignmentDataSource.deleteAssignment(id)
                .addOnSuccessListener(v -> load(universityDataSource))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
