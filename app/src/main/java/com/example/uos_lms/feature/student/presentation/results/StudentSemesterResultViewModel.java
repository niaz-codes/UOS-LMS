package com.example.uos_lms.feature.student.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.data.remote.api.ApiRepeatExamDataSource;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentSemesterResultViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;
    private final ApiRepeatExamDataSource repeatExamDataSource;
    private final String semesterId;

    private final MutableLiveData<StudentSemesterResultUiState> uiState =
            new MutableLiveData<>(StudentSemesterResultUiState.initial());

    @Inject
    public StudentSemesterResultViewModel(
            SavedStateHandle savedStateHandle,
            ApiExamResultDataSource examResultDataSource,
            ApiRepeatExamDataSource repeatExamDataSource) {
        this.examResultDataSource = examResultDataSource;
        this.repeatExamDataSource = repeatExamDataSource;
        this.semesterId = savedStateHandle.get("semesterId");
        load();
    }

    public LiveData<StudentSemesterResultUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches this semester's result (and repeat-exam statuses) from the backend without
     * blanking what's currently shown - no-ops while a refresh is already in flight. Recomputed
     * GPA/CGPA and any result correction show up immediately since this always re-fetches, never
     * reuses cached figures. */
    public void refresh() {
        StudentSemesterResultUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        fetch();
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        fetch();
    }

    private void fetch() {
        Tasks.whenAllSuccess(
                examResultDataSource.listSemesterResults(null, null, semesterId, null),
                repeatExamDataSource.listForSelf()
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            List<StudentSemesterResultSummary> semesterResults = (List<StudentSemesterResultSummary>) results.get(0);
            //noinspection unchecked
            List<RepeatExam> repeatExams = (List<RepeatExam>) results.get(1);
            bind(semesterResults, repeatExams);
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void bind(List<StudentSemesterResultSummary> semesterResults, List<RepeatExam> repeatExams) {
        if (semesterResults.isEmpty()) {
            uiState.setValue(uiState.getValue().toBuilder().hasResult(false).loading(false).refreshing(false).build());
            return;
        }
        StudentSemesterResultSummary semester = semesterResults.get(0);

        Map<String, com.example.uos_lms.core.domain.model.RepeatStatus> repeatStatusBySubjectId = new HashMap<>();
        for (RepeatExam repeatExam : repeatExams) {
            if (repeatExam.getSubjectId() == null) continue;
            repeatStatusBySubjectId.put(repeatExam.getSubjectId(), repeatExam.getRepeatStatus());
        }

        uiState.setValue(uiState.getValue().toBuilder()
                .hasResult(true)
                .semesterLabel(semester.getSemesterLabel())
                .semesterGpa(semester.getSemesterGpa())
                .cumulativeCgpa(semester.getCumulativeCgpa())
                .promotionStatus(semester.getPromotionStatus())
                .subjectResults(semester.getSubjectResults())
                .repeatStatusBySubjectId(repeatStatusBySubjectId)
                .loading(false)
                .refreshing(false)
                .build());
    }

    public void consumeError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
