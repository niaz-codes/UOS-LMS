package com.example.uos_lms.feature.admin.presentation.promotion;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.GradingApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.data.remote.api.dto.PromotionEnvelopeDto;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Admin's Student Semester Promotion flow: Department -> Session -> Current Semester, then
 * bulk-promote the resulting student roster into the next semester (number + 1 within the same
 * department). The backend recomputes failed-subject retakes and the 4-fail promotion block
 * itself (see backend/src/controllers/promotionController.js) - this screen just previews the
 * same failed-subjects signal for UX and fires one promote call per selected student, since the
 * new REST endpoint acts on a single student's own current semester rather than a Firestore
 * batch write.
 */
@HiltViewModel
public class AdminPromotionViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final ApiExamResultDataSource examResultDataSource;
    private final GradingApi gradingApi;

    private final MutableLiveData<AdminPromotionUiState> uiState = new MutableLiveData<>(AdminPromotionUiState.initial());

    private int rosterRequestId = 0;

    @Inject
    public AdminPromotionViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            ApiExamResultDataSource examResultDataSource,
            GradingApi gradingApi) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.examResultDataSource = examResultDataSource;
        this.gradingApi = gradingApi;

        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            List<Department> sorted = new ArrayList<>(departments);
            sorted.sort(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER));
            uiState.setValue(uiState.getValue().toBuilder().departments(sorted).build());
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public LiveData<AdminPromotionUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches the currently-selected semester's roster (or just no-ops if no semester is
     * chosen yet) without blanking what's shown - no-ops while a refresh is already in flight. */
    public void refresh() {
        AdminPromotionUiState state = uiState.getValue();
        if (state == null || state.isRefreshing()) return;
        if (state.getSelectedSemesterId() == null) return;
        uiState.setValue(state.toBuilder().refreshing(true).errorMessage(null).build());
        loadRoster(false);
    }

    public void selectDepartment(Department department) {
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedDepartmentId(department.getId())
                .selectedDepartmentName(department.getName())
                .sessions(Collections.emptyList())
                .selectedSessionId(null)
                .selectedSessionLabel(null)
                .semesters(Collections.emptyList())
                .selectedSemesterId(null)
                .currentSemesterNumber(0)
                .targetSemester(null)
                .rows(Collections.emptyList())
                .build());

        Tasks.whenAllSuccess(
                universityDataSource.listSessions(department.getId()),
                universityDataSource.listSemesters(department.getId())
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            List<Session> sessions = (List<Session>) results.get(0);
            List<Session> active = new ArrayList<>();
            for (Session session : sessions) {
                if (session.isActive()) active.add(session);
            }
            active.sort(Comparator.comparing(Session::getLabel));

            //noinspection unchecked
            List<Semester> semesters = new ArrayList<>((List<Semester>) results.get(1));
            semesters.sort(Comparator.comparingInt(Semester::getNumber));

            uiState.setValue(uiState.getValue().toBuilder().sessions(active).semesters(semesters).build());
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void selectSession(Session session) {
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedSessionId(session.getId())
                .selectedSessionLabel(session.getLabel())
                .selectedSemesterId(null)
                .currentSemesterNumber(0)
                .targetSemester(null)
                .rows(Collections.emptyList())
                .build());
    }

    public void selectSemester(Semester semester) {
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedSemesterId(semester.getId())
                .currentSemesterNumber(semester.getNumber())
                .build());
        resolveTargetSemester();
        loadRoster(true);
    }

    private void resolveTargetSemester() {
        AdminPromotionUiState state = uiState.getValue();
        if (state.getSelectedSemesterId() == null) return;
        Semester target = null;
        for (Semester semester : state.getSemesters()) {
            if (semester.getNumber() == state.getCurrentSemesterNumber() + 1) {
                target = semester;
                break;
            }
        }
        uiState.setValue(uiState.getValue().toBuilder().targetSemester(target).build());
    }

    private void loadRoster(boolean blankFirst) {
        AdminPromotionUiState state = uiState.getValue();
        String departmentId = state.getSelectedDepartmentId();
        String sessionId = state.getSelectedSessionId();
        String semesterId = state.getSelectedSemesterId();
        if (departmentId == null || sessionId == null || semesterId == null) return;

        final int requestId = ++rosterRequestId;
        AdminPromotionUiState.AdminPromotionUiStateBuilder starting = uiState.getValue().toBuilder().loadingRoster(true);
        if (blankFirst) starting.rows(Collections.emptyList());
        uiState.setValue(starting.build());

        userDataSource.listStudentsInSession(departmentId, sessionId, semesterId)
                .onSuccessTask(students -> buildRows(students, semesterId))
                .addOnSuccessListener(rows -> {
                    if (requestId != rosterRequestId) return;
                    uiState.setValue(uiState.getValue().toBuilder().rows(rows).loadingRoster(false).refreshing(false).build());
                })
                .addOnFailureListener(e -> {
                    if (requestId != rosterRequestId) return;
                    uiState.setValue(uiState.getValue().toBuilder().loadingRoster(false).refreshing(false).errorMessage(e.getMessage()).build());
                });
    }

    private Task<List<PromotionStudentRow>> buildRows(List<User> students, String semesterId) {
        List<User> approved = new ArrayList<>();
        for (User student : students) {
            if (student.getStatus() == UserStatus.APPROVED) approved.add(student);
        }
        if (approved.isEmpty()) return Tasks.forResult(Collections.emptyList());

        List<Task<List<ExamResult>>> resultTasks = new ArrayList<>();
        for (User student : approved) {
            resultTasks.add(examResultDataSource.listApprovedForStudentInSemester(student.getUid(), semesterId));
        }

        return Tasks.whenAllSuccess(resultTasks).onSuccessTask(allResults -> {
            List<PromotionStudentRow> rows = new ArrayList<>();
            for (int i = 0; i < approved.size(); i++) {
                User student = approved.get(i);
                @SuppressWarnings("unchecked")
                List<ExamResult> results = (List<ExamResult>) allResults.get(i);
                List<ExamResult> failed = new ArrayList<>();
                for (ExamResult result : results) {
                    if ("F".equals(result.getGrade())) failed.add(result);
                }
                rows.add(PromotionStudentRow.builder()
                        .student(student)
                        .failedResults(failed)
                        .hasResults(!results.isEmpty())
                        // A promoted student's currentSemesterId moves forward immediately
                        // server-side, so they stop matching this roster query entirely -
                        // "already promoted" can no longer happen within a loaded roster.
                        .alreadyPromoted(false)
                        .selected(true)
                        .build());
            }
            rows.sort(Comparator.comparing(row -> row.getStudent().getFullName(), String.CASE_INSENSITIVE_ORDER));
            return Tasks.forResult(rows);
        });
    }

    public void toggleSelected(String uid) {
        List<PromotionStudentRow> rows = new ArrayList<>();
        for (PromotionStudentRow row : uiState.getValue().getRows()) {
            if (row.getStudent().getUid().equals(uid) && !row.isAlreadyPromoted()) {
                rows.add(row.toBuilder().selected(!row.isSelected()).build());
            } else {
                rows.add(row);
            }
        }
        uiState.setValue(uiState.getValue().toBuilder().rows(rows).build());
    }

    /** One promote call per selected student (the endpoint acts on that student's own
     * current semester - no batched multi-student write anymore). Partial failures (e.g. a
     * 403 from the 4-fail block on a student whose result changed since the roster loaded)
     * are reported without blocking the students that did succeed. */
    public void promote() {
        AdminPromotionUiState state = uiState.getValue();
        Semester target = state.getTargetSemester();
        if (target == null) return;

        List<PromotionStudentRow> selectedRows = new ArrayList<>();
        for (PromotionStudentRow row : state.getRows()) {
            if (row.isSelected() && !row.isAlreadyPromoted()) selectedRows.add(row);
        }
        if (selectedRows.isEmpty()) {
            uiState.setValue(state.toBuilder().errorMessage("Select at least one student to promote.").build());
            return;
        }

        uiState.setValue(uiState.getValue().toBuilder().promoting(true).errorMessage(null).build());

        List<Task<PromotionEnvelopeDto>> promoteTasks = new ArrayList<>();
        for (PromotionStudentRow row : selectedRows) {
            promoteTasks.add(RetrofitTasks.call(gradingApi.promote(row.getStudent().getUid())));
        }

        Tasks.whenAllComplete(promoteTasks).addOnSuccessListener(completedTasks -> {
            int succeeded = 0;
            String lastError = null;
            for (Task<?> task : completedTasks) {
                if (task.isSuccessful()) {
                    succeeded++;
                } else if (task.getException() != null) {
                    lastError = task.getException().getMessage();
                }
            }
            String actionMessage = succeeded + " of " + selectedRows.size() + " student(s) promoted to " + target.getDisplayName() + ".";
            String errorMessage = (succeeded < selectedRows.size() && lastError != null)
                    ? "Some promotions failed: " + lastError : null;
            uiState.setValue(uiState.getValue().toBuilder()
                    .promoting(false).actionMessage(actionMessage).errorMessage(errorMessage).build());
            loadRoster(true);
        });
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
