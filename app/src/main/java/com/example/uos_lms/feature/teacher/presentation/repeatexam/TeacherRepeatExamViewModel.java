package com.example.uos_lms.feature.teacher.presentation.repeatexam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.data.remote.api.ApiRepeatExamDataSource;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.domain.model.RepeatStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** A subject's failed, repeat-eligible results, one at a time - marks entry mirrors
 * TeacherExamResultFragment's row pattern, but scoped to only the students who need a
 * repeat rather than the whole roster. Scheduling the RepeatExam and submitting its marks
 * are two backend calls (see ApiRepeatExamDataSource) collapsed into one Teacher action. */
@HiltViewModel
public class TeacherRepeatExamViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;
    private final ApiRepeatExamDataSource repeatExamDataSource;

    private final String subjectId;
    private final String subjectCode;
    private final String subjectTitle;

    private final MutableLiveData<TeacherRepeatExamUiState> uiState = new MutableLiveData<>(TeacherRepeatExamUiState.initial());

    private List<ExamResult> latestResults = Collections.emptyList();
    private List<RepeatExam> latestRepeats = Collections.emptyList();

    @Inject
    public TeacherRepeatExamViewModel(
            SavedStateHandle savedStateHandle,
            ApiExamResultDataSource examResultDataSource,
            ApiRepeatExamDataSource repeatExamDataSource) {
        this.examResultDataSource = examResultDataSource;
        this.repeatExamDataSource = repeatExamDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        this.subjectCode = savedStateHandle.get("subjectCode");
        this.subjectTitle = savedStateHandle.get("subjectTitle");
        load();
    }

    public LiveData<TeacherRepeatExamUiState> getUiState() {
        return uiState;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getSubjectTitle() {
        return subjectTitle;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        Tasks.whenAllSuccess(
                examResultDataSource.listRepeatEligibleForSubject(subjectId),
                repeatExamDataSource.listForSubject(subjectId)
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            latestResults = (List<ExamResult>) results.get(0);
            //noinspection unchecked
            latestRepeats = (List<RepeatExam>) results.get(1);
            recompute();
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                .loading(false).errorMessage(e.getMessage()).build()));
    }

    private void recompute() {
        List<ExamResult> sorted = new ArrayList<>(latestResults);
        sorted.sort(Comparator.comparing(ExamResult::getStudentName, String.CASE_INSENSITIVE_ORDER));

        List<RepeatExamRow> rows = new ArrayList<>();
        for (ExamResult result : sorted) {
            RepeatExam latest = null;
            for (RepeatExam repeat : latestRepeats) {
                if (repeat.getExamResultId().equals(result.getId())) {
                    if (latest == null || repeat.getCreatedAt() > latest.getCreatedAt()) latest = repeat;
                }
            }
            String marksInput = latest != null && latest.getRepeatStatus() != RepeatStatus.SUBMITTED && latest.getNewMarks() != null
                    ? String.valueOf(latest.getNewMarks().intValue()) : "";
            rows.add(RepeatExamRow.builder().examResult(result).latestRepeat(latest).marksInput(marksInput).build());
        }
        uiState.setValue(uiState.getValue().toBuilder().rows(rows).loading(false).build());
    }

    public void updateMarks(String examResultId, String value) {
        if (!isValidDigits(value)) return;
        TeacherRepeatExamUiState state = uiState.getValue();
        List<RepeatExamRow> rows = new ArrayList<>();
        for (RepeatExamRow row : state.getRows()) {
            if (row.getExamResult().getId().equals(examResultId)) {
                rows.add(row.toBuilder().marksInput(value).build());
            } else {
                rows.add(row);
            }
        }
        uiState.setValue(state.toBuilder().rows(rows).build());
    }

    private boolean isValidDigits(String value) {
        if (value.length() > 3) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    public void submit(String examResultId) {
        TeacherRepeatExamUiState state = uiState.getValue();
        RepeatExamRow target = null;
        for (RepeatExamRow row : state.getRows()) {
            if (row.getExamResult().getId().equals(examResultId)) {
                target = row;
                break;
            }
        }
        if (target == null) return;

        Integer marks = parseIntOrNull(target.getMarksInput());
        if (marks == null || marks < 0 || marks > 100) {
            uiState.setValue(state.toBuilder().errorMessage("Enter a valid mark (0-100) first.").build());
            return;
        }

        uiState.setValue(state.toBuilder().processingExamResultId(examResultId).errorMessage(null).build());

        Task<RepeatExam> chain = target.needsFreshRepeat()
                ? repeatExamDataSource.create(examResultId)
                        .continueWithTask(createTask -> repeatExamDataSource.submitMarks(createTask.getResult().getId(), marks))
                : repeatExamDataSource.submitMarks(target.getLatestRepeat().getId(), marks);

        chain.addOnSuccessListener(repeatExam -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .processingExamResultId(null)
                            .actionMessage("Repeat marks submitted for HOD approval.")
                            .build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .processingExamResultId(null).errorMessage(e.getMessage()).build()));
    }

    @androidx.annotation.Nullable
    private static Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
