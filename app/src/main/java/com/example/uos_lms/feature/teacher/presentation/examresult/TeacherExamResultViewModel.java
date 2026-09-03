package com.example.uos_lms.feature.teacher.presentation.examresult;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.dto.StudentMarksDto;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherExamResultViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;
    private final ApiUniversityDataSource universityDataSource;

    private final String subjectId;
    private final String subjectCode;
    private final String subjectTitle;
    private final int creditHours;
    private final String departmentId;
    private final String semesterId;
    private final String teacherUid;
    private final String teacherName;

    private final MediatorLiveData<TeacherExamResultUiState> uiState =
            new MediatorLiveData<>(TeacherExamResultUiState.initial());

    private List<User> latestStudents = Collections.emptyList();
    private List<ExamResult> latestResults = Collections.emptyList();

    @Inject
    public TeacherExamResultViewModel(
            SavedStateHandle savedStateHandle,
            ApiExamResultDataSource examResultDataSource,
            ApiUniversityDataSource universityDataSource,
            SessionManager sessionManager) {
        this.examResultDataSource = examResultDataSource;
        this.universityDataSource = universityDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        this.subjectCode = savedStateHandle.get("subjectCode");
        this.subjectTitle = savedStateHandle.get("subjectTitle");
        Integer credit = savedStateHandle.get("creditHours");
        this.creditHours = credit != null ? credit : 0;
        this.departmentId = savedStateHandle.get("departmentId");
        this.semesterId = savedStateHandle.get("semesterId");

        CachedSession session = sessionManager.getCachedSession().getValue();
        this.teacherUid = session != null ? session.getUid() : null;
        this.teacherName = session != null ? session.getFullName() : "";

        load();
    }

    public LiveData<TeacherExamResultUiState> getUiState() {
        return uiState;
    }

    /** Roster (enrolled + retake students) and this subject's existing results are fetched
     * in parallel, one shot - there's no more real-time listener to fall back on, so the
     * Fragment calls this again on refresh/resume. */
    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        Tasks.whenAllSuccess(
                universityDataSource.subjectRoster(subjectId),
                examResultDataSource.listResultsForSubject(subjectId)
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            latestStudents = (List<User>) results.get(0);
            //noinspection unchecked
            latestResults = (List<ExamResult>) results.get(1);
            recompute();
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                .loading(false).errorMessage(e.getMessage()).build()));
    }

    private void recompute() {
        List<User> sortedStudents = new ArrayList<>(latestStudents);
        sortedStudents.sort(Comparator.comparing(User::getFullName));

        List<ExamResultRow> rows = new ArrayList<>();
        Integer existingTotal = null;
        for (User student : sortedStudents) {
            ExamResult existing = null;
            for (ExamResult result : latestResults) {
                if (result.getStudentUid().equals(student.getUid())) {
                    existing = result;
                    break;
                }
            }
            String marksInput = existing != null ? String.valueOf(existing.getObtainedMarks()) : "";
            rows.add(ExamResultRow.builder().student(student).existing(existing).marksInput(marksInput).build());
            if (existing != null && existingTotal == null) existingTotal = existing.getTotalMarks();
        }

        TeacherExamResultUiState current = uiState.getValue();
        String totalMarksInput = existingTotal != null ? String.valueOf(existingTotal) : current.getTotalMarksInput();
        uiState.setValue(current.toBuilder().rows(rows).totalMarksInput(totalMarksInput).loading(false).build());
    }

    public void updateTotalMarks(String value) {
        TeacherExamResultUiState state = uiState.getValue();
        if (state.isTotalMarksLocked()) return;
        if (!isValidDigits(value)) return;
        uiState.setValue(state.toBuilder().totalMarksInput(value).build());
    }

    public void updateMarks(String studentUid, String value) {
        if (!isValidDigits(value)) return;
        TeacherExamResultUiState state = uiState.getValue();
        List<ExamResultRow> rows = new ArrayList<>();
        for (ExamResultRow row : state.getRows()) {
            if (row.getStudent().getUid().equals(studentUid)) {
                rows.add(row.toBuilder().marksInput(value).build());
            } else {
                rows.add(row);
            }
        }
        uiState.setValue(state.toBuilder().rows(rows).build());
    }

    private boolean isValidDigits(String value) {
        if (value.length() > 4) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    public void saveDraft() {
        if (teacherUid == null) {
            setError("Session expired. Please log in again.");
            return;
        }
        TeacherExamResultUiState state = uiState.getValue();
        Integer totalMarks = TeacherExamResultUiState.parseIntOrNull(state.getTotalMarksInput());
        if (totalMarks == null || totalMarks <= 0) {
            setError("Enter a valid total marks value first.");
            return;
        }
        List<StudentMarksDto> toSave = new ArrayList<>();
        for (ExamResultRow row : state.getEditableRows()) {
            Integer marks = TeacherExamResultUiState.parseIntOrNull(row.getMarksInput());
            if (marks == null || marks < 0 || marks > totalMarks) continue;
            toSave.add(toPercentageMarks(row, marks, totalMarks));
        }
        if (toSave.isEmpty()) {
            setError("Enter marks for at least one student first.");
            return;
        }

        uiState.setValue(state.toBuilder().saving(true).errorMessage(null).build());
        examResultDataSource.saveDrafts(subjectId, toSave)
                .addOnSuccessListener(results -> {
                    latestResults = results;
                    recompute();
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage("Draft saved.").build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().saving(false).errorMessage(e.getMessage()).build()));
    }

    public void submitForApproval() {
        if (teacherUid == null) {
            setError("Session expired. Please log in again.");
            return;
        }
        TeacherExamResultUiState state = uiState.getValue();
        if (!state.canSubmit()) {
            setError("Enter valid marks for every student before submitting.");
            return;
        }
        int totalMarks = TeacherExamResultUiState.parseIntOrNull(state.getTotalMarksInput());
        List<StudentMarksDto> toSubmit = new ArrayList<>();
        for (ExamResultRow row : state.getEditableRows()) {
            int marks = TeacherExamResultUiState.parseIntOrNull(row.getMarksInput());
            toSubmit.add(toPercentageMarks(row, marks, totalMarks));
        }

        uiState.setValue(state.toBuilder().saving(true).errorMessage(null).build());
        examResultDataSource.submitForApproval(subjectId, toSubmit)
                .addOnSuccessListener(results -> {
                    latestResults = results;
                    recompute();
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage("Submitted for HOD approval.").build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().saving(false).errorMessage(e.getMessage()).build()));
    }

    /** The backend takes marks as a 0-100 value directly (see spec §6) - the Teacher's
     * customizable "Total Marks" (e.g. a quiz out of 20) is scaled to a percentage here via
     * the same getPercentage() the rest of the app already uses to display/grade a result. */
    private StudentMarksDto toPercentageMarks(ExamResultRow row, int marks, int totalMarks) {
        ExamResult scratch = buildResult(row, marks, totalMarks, teacherUid);
        return new StudentMarksDto(row.getStudent().getUid(), scratch.getPercentage());
    }

    private ExamResult buildResult(ExamResultRow row, int marks, int totalMarks, String teacherUid) {
        ExamResult existing = row.getExisting();
        return ExamResult.builder()
                .id(existing != null ? existing.getId() : "")
                .subjectId(subjectId)
                .subjectCode(subjectCode)
                .subjectTitle(subjectTitle)
                .creditHours(creditHours)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .teacherUid(teacherUid)
                .teacherName(teacherName)
                .studentUid(row.getStudent().getUid())
                .studentName(row.getStudent().getFullName())
                .studentRollNumber(row.getStudent().getRollNumber())
                .obtainedMarks(marks)
                .totalMarks(totalMarks)
                .status(ResultStatus.DRAFT)
                .createdAt(existing != null ? existing.getCreatedAt() : 0L)
                .build();
    }

    private void setError(String message) {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(message).build());
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
