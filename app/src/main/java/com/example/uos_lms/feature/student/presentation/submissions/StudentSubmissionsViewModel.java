package com.example.uos_lms.feature.student.presentation.submissions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.example.uos_lms.core.domain.model.Subject;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentSubmissionsViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiAssignmentDataSource assignmentDataSource;

    private final MutableLiveData<StudentSubmissionsUiState> uiState =
            new MutableLiveData<>(StudentSubmissionsUiState.initial());

    private List<Subject> latestSubjects;
    private final Map<String, List<Assignment>> assignmentsBySubject = new HashMap<>();
    private List<AssignmentSubmission> latestSubmissions = Collections.emptyList();

    @Inject
    public StudentSubmissionsViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiAssignmentDataSource assignmentDataSource) {
        this.universityDataSource = universityDataSource;
        this.assignmentDataSource = assignmentDataSource;
        load();
    }

    public LiveData<StudentSubmissionsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches assignments + submissions from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight. Also the entry point for
     * refreshing automatically when returning from Submit Assignment (see Fragment.onResume). */
    public void refresh() {
        StudentSubmissionsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        universityDataSource.mySubjects()
                .addOnSuccessListener(subjects -> {
                    latestSubjects = subjects;
                    List<Task<List<Assignment>>> tasks = new ArrayList<>();
                    for (Subject subject : subjects) {
                        assignmentsBySubject.put(subject.getId(), Collections.emptyList());
                        tasks.add(assignmentDataSource.assignmentsForSubject(subject.getId())
                                .addOnSuccessListener(assignments -> assignmentsBySubject.put(subject.getId(), assignments)));
                    }
                    Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recompute());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));

        assignmentDataSource.submissionsForCurrentStudent()
                .addOnSuccessListener(submissions -> {
                    latestSubmissions = submissions;
                    recompute();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void recompute() {
        if (latestSubjects == null) return;

        Map<String, AssignmentSubmission> submissionsByAssignment = new HashMap<>();
        for (AssignmentSubmission submission : latestSubmissions) {
            submissionsByAssignment.put(submission.getAssignmentId(), submission);
        }

        List<Assignment> allAssignments = new ArrayList<>();
        for (Subject subject : latestSubjects) {
            List<Assignment> assignments = assignmentsBySubject.get(subject.getId());
            if (assignments != null) allAssignments.addAll(assignments);
        }
        allAssignments.sort(Comparator.comparingLong(Assignment::getDueDateMillis).reversed());

        List<SubmissionRow> rows = new ArrayList<>();
        for (Assignment assignment : allAssignments) {
            rows.add(SubmissionRow.builder()
                    .assignment(assignment)
                    .submission(submissionsByAssignment.get(assignment.getId()))
                    .build());
        }

        uiState.setValue(uiState.getValue().toBuilder().rows(rows).loading(false).refreshing(false).build());
    }
}
