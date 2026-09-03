package com.example.uos_lms.feature.admin.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.data.remote.api.ApiRepeatExamDataSource;
import com.example.uos_lms.core.domain.model.PromotionStatus;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.domain.model.RepeatStatus;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;
import com.example.uos_lms.core.domain.model.SubjectResultSnapshot;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import lombok.Builder;
import lombok.Getter;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminStudentSemesterResultViewModel extends ViewModel {

    @Getter
    @Builder(toBuilder = true)
    public static class UiState {
        @Builder.Default
        private final boolean hasResult = false;
        @Builder.Default
        private final double semesterGpa = 0.0;
        @Builder.Default
        private final double cumulativeCgpa = 0.0;
        @Builder.Default
        private final PromotionStatus promotionStatus = PromotionStatus.NOT_EVALUATED;
        @Builder.Default
        private final List<SubjectResultSnapshot> subjectResults = Collections.emptyList();
        @Builder.Default
        private final Map<String, RepeatStatus> repeatStatusBySubjectId = Collections.emptyMap();
        @Builder.Default
        private final boolean loading = true;
        private final String errorMessage;

        public boolean isAllPassed() {
            return subjectResults.stream().allMatch(s -> s.getStatus() == com.example.uos_lms.core.domain.model.SubjectResultStatus.PASS);
        }

        static UiState initial() {
            return UiState.builder().build();
        }
    }

    private final ApiExamResultDataSource examResultDataSource;
    private final ApiRepeatExamDataSource repeatExamDataSource;
    private final String studentId;
    private final String semesterId;

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.initial());

    @Inject
    public AdminStudentSemesterResultViewModel(
            SavedStateHandle savedStateHandle,
            ApiExamResultDataSource examResultDataSource,
            ApiRepeatExamDataSource repeatExamDataSource) {
        this.examResultDataSource = examResultDataSource;
        this.repeatExamDataSource = repeatExamDataSource;
        this.studentId = savedStateHandle.get("studentId");
        this.semesterId = savedStateHandle.get("semesterId");
        load();
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        Tasks.whenAllSuccess(
                examResultDataSource.listSemesterResults(null, null, semesterId, studentId),
                repeatExamDataSource.listForStudent(studentId)
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            List<StudentSemesterResultSummary> semesterResults = (List<StudentSemesterResultSummary>) results.get(0);
            //noinspection unchecked
            List<RepeatExam> repeatExams = (List<RepeatExam>) results.get(1);
            bind(semesterResults, repeatExams);
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    private void bind(List<StudentSemesterResultSummary> semesterResults, List<RepeatExam> repeatExams) {
        if (semesterResults.isEmpty()) {
            uiState.setValue(uiState.getValue().toBuilder().hasResult(false).loading(false).build());
            return;
        }
        StudentSemesterResultSummary semester = semesterResults.get(0);

        Map<String, RepeatStatus> repeatStatusBySubjectId = new HashMap<>();
        for (RepeatExam repeatExam : repeatExams) {
            if (repeatExam.getSubjectId() == null) continue;
            repeatStatusBySubjectId.put(repeatExam.getSubjectId(), repeatExam.getRepeatStatus());
        }

        uiState.setValue(uiState.getValue().toBuilder()
                .hasResult(true)
                .semesterGpa(semester.getSemesterGpa())
                .cumulativeCgpa(semester.getCumulativeCgpa())
                .promotionStatus(semester.getPromotionStatus())
                .subjectResults(semester.getSubjectResults())
                .repeatStatusBySubjectId(repeatStatusBySubjectId)
                .loading(false)
                .build());
    }
}
