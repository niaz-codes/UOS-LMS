package com.example.uos_lms.feature.teacher.presentation.examresult.workspace;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.data.remote.api.ApiRepeatExamDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.dto.StudentMarksDto;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ExamResultRosterRow;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Dedicated Exam Result workspace (bottom-nav screen): a "My Assigned Courses" grid (built
 * from the teacher's own subjects + roster sizes + their own submitted ExamResult rows - no
 * dedicated backend endpoint needed) that opens directly into a spreadsheet for bulk marks
 * entry, repeat-exam support, and a live result summary. The original tab-style
 * Department -> Session -> Semester -> Subject -> Exam Type selection plumbing still backs the
 * roster loading underneath, it's just no longer the entry point (see the Fragment). */
@HiltViewModel
public class TeacherExamResultWorkspaceViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;
    private final ApiRepeatExamDataSource repeatExamDataSource;
    private final ApiUniversityDataSource universityDataSource;

    private final MediatorLiveData<TeacherExamResultWorkspaceUiState> uiState =
            new MediatorLiveData<>(TeacherExamResultWorkspaceUiState.builder().build());

    private final Map<String, String> courseDepartmentNamesById = new HashMap<>();
    private final Map<String, List<Semester>> courseSemestersByDept = new HashMap<>();
    private final Map<String, String> courseSessionLabelsById = new HashMap<>();

    @Inject
    public TeacherExamResultWorkspaceViewModel(
            ApiExamResultDataSource examResultDataSource,
            ApiRepeatExamDataSource repeatExamDataSource,
            ApiUniversityDataSource universityDataSource) {
        this.examResultDataSource = examResultDataSource;
        this.repeatExamDataSource = repeatExamDataSource;
        this.universityDataSource = universityDataSource;
        loadOptions();
        loadCourseOverview();
    }

    public LiveData<TeacherExamResultWorkspaceUiState> getUiState() {
        return uiState;
    }

    // ---- "My Assigned Courses" overview ----

    public void loadCourseOverview() {
        uiState.setValue(uiState.getValue().toBuilder().loadingCourses(true).coursesError(null).build());
        universityDataSource.listSubjectsForTeacher("me")
                .addOnSuccessListener(this::loadCourseMetadata)
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .loadingCourses(false).coursesError(e.getMessage()).build()));
    }

    private void loadCourseMetadata(List<Subject> subjects) {
        if (subjects.isEmpty()) {
            uiState.setValue(uiState.getValue().toBuilder().loadingCourses(false).courses(Collections.emptyList()).build());
            return;
        }
        Set<String> departmentIds = new HashSet<>();
        for (Subject subject : subjects) departmentIds.add(subject.getDepartmentId());

        List<Task<?>> tasks = new ArrayList<>();
        Map<String, List<User>> rosterBySubject = new HashMap<>();
        Map<String, Set<String>> sessionIdsBySubject = new HashMap<>();

        tasks.add(universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            for (Department department : departments) {
                if (departmentIds.contains(department.getId())) courseDepartmentNamesById.put(department.getId(), department.getName());
            }
        }));
        for (String departmentId : departmentIds) {
            tasks.add(universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> courseSemestersByDept.put(departmentId, semesters)));
            tasks.add(universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions -> {
                for (Session session : sessions) courseSessionLabelsById.put(session.getId(), session.getLabel());
            }));
        }
        for (Subject subject : subjects) {
            String subjectId = subject.getId();
            tasks.add(universityDataSource.subjectRoster(subjectId).addOnSuccessListener(students -> {
                rosterBySubject.put(subjectId, students);
                Set<String> sessionIds = new HashSet<>();
                for (User student : students) {
                    if (student.getSessionId() != null && !student.getSessionId().isEmpty()) sessionIds.add(student.getSessionId());
                }
                sessionIdsBySubject.put(subjectId, sessionIds);
            }));
        }
        Task<List<ExamResult>> resultsTask = examResultDataSource.listForCurrentRole();
        tasks.add(resultsTask);

        Tasks.whenAllComplete(tasks).addOnSuccessListener(ignored -> {
            List<ExamResult> results;
            try {
                results = resultsTask.isSuccessful() && resultsTask.getResult() != null
                        ? resultsTask.getResult() : Collections.emptyList();
            } catch (RuntimeException e) {
                results = Collections.emptyList();
            }
            combineCourses(subjects, rosterBySubject, sessionIdsBySubject, results);
        });
    }

    private void combineCourses(List<Subject> subjects, Map<String, List<User>> rosterBySubject,
            Map<String, Set<String>> sessionIdsBySubject, List<ExamResult> results) {
        Map<String, List<ExamResult>> resultsBySubject = new HashMap<>();
        for (ExamResult result : results) {
            resultsBySubject.computeIfAbsent(result.getSubjectId(), k -> new ArrayList<>()).add(result);
        }

        List<TeacherCourseSummary> summaries = new ArrayList<>();
        for (Subject subject : subjects) {
            List<User> roster = rosterBySubject.getOrDefault(subject.getId(), Collections.emptyList());
            List<ExamResult> subjectResults = resultsBySubject.getOrDefault(subject.getId(), Collections.emptyList());

            int draft = 0, pending = 0, approved = 0, rejected = 0;
            for (ExamResult result : subjectResults) {
                switch (result.getStatus()) {
                    case DRAFT: draft++; break;
                    case PENDING_HOD_APPROVAL: pending++; break;
                    case APPROVED: approved++; break;
                    case REJECTED: rejected++; break;
                }
            }

            String semesterLabel = "Semester —";
            List<Semester> deptSemesters = courseSemestersByDept.get(subject.getDepartmentId());
            if (deptSemesters != null) {
                for (Semester semester : deptSemesters) {
                    if (semester.getId().equals(subject.getSemesterId())) {
                        semesterLabel = semester.getDisplayName();
                        break;
                    }
                }
            }
            String departmentName = courseDepartmentNamesById.getOrDefault(subject.getDepartmentId(), "—");

            summaries.add(TeacherCourseSummary.builder()
                    .subject(subject)
                    .departmentName(departmentName)
                    .semesterLabel(semesterLabel)
                    .sessionLabel(buildCourseSessionLabel(sessionIdsBySubject.get(subject.getId())))
                    .totalStudents(roster.size())
                    .resultsEntered(subjectResults.size())
                    .resultsDraft(draft)
                    .resultsPending(pending)
                    .resultsApproved(approved)
                    .resultsRejected(rejected)
                    .build());
        }
        summaries.sort((a, b) -> a.getSubject().getTitle().compareToIgnoreCase(b.getSubject().getTitle()));

        uiState.setValue(uiState.getValue().toBuilder().loadingCourses(false).courses(summaries).build());
    }

    private String buildCourseSessionLabel(Set<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return "All Sessions";
        List<String> labels = new ArrayList<>();
        for (String sessionId : sessionIds) {
            String label = courseSessionLabelsById.get(sessionId);
            if (label != null && !label.isEmpty()) labels.add(label);
        }
        if (labels.isEmpty()) return "All Sessions";
        labels.sort(String::compareTo);
        return String.join(", ", labels);
    }

    /** Opens a course straight into its student-results sheet - the course card already fully
     * identifies the subject, so this skips the department/session/semester tab cascade and
     * loads the roster directly (sessionId left null = every session's students, matching what
     * "the course's students" means from a course card). */
    public void openCourse(TeacherCourseSummary course) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        uiState.setValue(state.toBuilder()
                .showCourseGrid(false)
                .activeCourse(course)
                .selectedDepartmentId(course.getSubject().getDepartmentId())
                .selectedSemesterId(course.getSubject().getSemesterId())
                .selectedSessionId(null)
                .selectedSubjectId(course.getSubject().getId())
                .selectedExamType(TeacherExamResultWorkspaceUiState.EXAM_TYPE_CURRENT)
                .totalMarksInput("100")
                .studentSearchQuery("")
                .studentFilter(TeacherExamResultWorkspaceUiState.StudentFilter.ALL)
                .studentSort(TeacherExamResultWorkspaceUiState.StudentSort.ROLL)
                .rows(Collections.emptyList())
                .sectionError(null)
                .build());
        loadRoster();
    }

    public void backToCourseGrid() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        uiState.setValue(state.toBuilder()
                .showCourseGrid(true)
                .activeCourse(null)
                .selectedSubjectId(null)
                .rows(Collections.emptyList())
                .build());
        loadCourseOverview();
    }

    // ---- Roster search / filter / sort ----

    public void setStudentSearchQuery(String query) {
        uiState.setValue(uiState.getValue().toBuilder().studentSearchQuery(query).build());
    }

    public void setStudentFilter(TeacherExamResultWorkspaceUiState.StudentFilter filter) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (filter == state.getStudentFilter()) return;
        uiState.setValue(state.toBuilder().studentFilter(filter).build());
    }

    public void setStudentSort(TeacherExamResultWorkspaceUiState.StudentSort sort) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (sort == state.getStudentSort()) return;
        uiState.setValue(state.toBuilder().studentSort(sort).build());
    }

    // ---- Options (departments + sessions + semesters) ----

    public void loadOptions() {
        uiState.setValue(uiState.getValue().toBuilder().loadingOptions(true).optionsError(null).build());
        examResultDataSource.loadExamResultOptions()
                .addOnSuccessListener(options -> uiState.setValue(uiState.getValue().toBuilder()
                        .loadingOptions(false).departmentOptions(options).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .loadingOptions(false).optionsError(e.getMessage()).build()));
    }

    // ---- Selections ----

    public void onDepartmentSelected(String departmentId) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (departmentId == null || departmentId.equals(state.getSelectedDepartmentId())) return;
        uiState.setValue(state.toBuilder()
                .selectedDepartmentId(departmentId)
                .selectedSessionId(null)
                .selectedSemesterId(null)
                .selectedSubjectId(null)
                .subjects(Collections.emptyList())
                .rows(Collections.emptyList())
                .sectionError(null)
                .build());
    }

    public void onSessionSelected(String sessionId) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (sessionId == null || sessionId.equals(state.getSelectedSessionId())) return;
        uiState.setValue(state.toBuilder()
                .selectedSessionId(sessionId)
                .selectedSemesterId(null)
                .selectedSubjectId(null)
                .subjects(Collections.emptyList())
                .rows(Collections.emptyList())
                .sectionError(null)
                .build());
    }

    public void onSemesterSelected(String semesterId) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (semesterId == null || semesterId.equals(state.getSelectedSemesterId())) return;
        uiState.setValue(state.toBuilder()
                .selectedSemesterId(semesterId)
                .selectedSubjectId(null)
                .rows(Collections.emptyList())
                .sectionError(null)
                .build());
        loadSubjects();
    }

    public void onSubjectSelected(String subjectId) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (subjectId == null || subjectId.equals(state.getSelectedSubjectId())) return;
        uiState.setValue(state.toBuilder()
                .selectedSubjectId(subjectId)
                .rows(Collections.emptyList())
                .sectionError(null)
                .build());
        loadRoster();
    }

    public void onExamTypeSelected(String examType) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (examType == null || examType.equals(state.getSelectedExamType())) return;
        uiState.setValue(state.toBuilder().selectedExamType(examType).rows(Collections.emptyList()).build());
        loadRoster();
    }

    // ---- Data loading ----

    private void loadSubjects() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        String departmentId = state.getSelectedDepartmentId();
        String sessionId = state.getSelectedSessionId();
        String semesterId = state.getSelectedSemesterId();
        if (departmentId == null || sessionId == null || semesterId == null) return;
        uiState.setValue(state.toBuilder().loadingSubjects(true).sectionError(null).build());
        examResultDataSource.listWorkspaceSubjects(departmentId, semesterId, sessionId)
                .addOnSuccessListener(subjects -> {
                    TeacherExamResultWorkspaceUiState current = uiState.getValue();
                    if (!semesterId.equals(current.getSelectedSemesterId())) return;
                    String selectedSubjectId = current.getSelectedSubjectId();
                    boolean stillSelected = selectedSubjectId != null && containsId(subjects, selectedSubjectId);
                    uiState.setValue(current.toBuilder()
                            .loadingSubjects(false)
                            .subjects(subjects)
                            .selectedSubjectId(stillSelected ? selectedSubjectId : null)
                            .rows(stillSelected ? current.getRows() : Collections.emptyList())
                            .build());
                })
                .addOnFailureListener(e -> {
                    TeacherExamResultWorkspaceUiState current = uiState.getValue();
                    if (!semesterId.equals(current.getSelectedSemesterId())) return;
                    uiState.setValue(current.toBuilder()
                            .loadingSubjects(false).sectionError(e.getMessage()).build());
                });
    }

    private void loadRoster() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        String subjectId = state.getSelectedSubjectId();
        if (subjectId == null) return;
        String sessionId = state.getSelectedSessionId();
        String examType = state.getSelectedExamType();
        uiState.setValue(state.toBuilder().loadingRoster(true).rows(Collections.emptyList()).sectionError(null).build());
        examResultDataSource.loadExamResultRoster(subjectId, sessionId, examType)
                .addOnSuccessListener(roster -> {
                    TeacherExamResultWorkspaceUiState current = uiState.getValue();
                    if (!subjectId.equals(current.getSelectedSubjectId()) || !examType.equals(current.getSelectedExamType())) {
                        return;
                    }
                    uiState.setValue(current.toBuilder()
                            .loadingRoster(false).rows(buildRows(roster, examType)).build());
                })
                .addOnFailureListener(e -> {
                    TeacherExamResultWorkspaceUiState current = uiState.getValue();
                    if (!subjectId.equals(current.getSelectedSubjectId()) || !examType.equals(current.getSelectedExamType())) {
                        return;
                    }
                    uiState.setValue(current.toBuilder()
                            .loadingRoster(false).sectionError(e.getMessage()).build());
                });
    }

    private static List<ResultEntryRow> buildRows(List<ExamResultRosterRow> roster, String examType) {
        if (roster == null) return Collections.emptyList();
        List<ResultEntryRow> rows = new ArrayList<>();
        for (ExamResultRosterRow source : roster) {
            String marksInput = "";
            if (TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(examType)) {
                RepeatExam repeat = source.getRepeatExam();
                if (repeat != null && repeat.isEditable() && repeat.getNewMarks() != null) {
                    marksInput = String.valueOf(repeat.getNewMarks().intValue());
                }
            } else if (source.getResult() != null) {
                marksInput = String.valueOf(source.getResult().getObtainedMarks());
            }
            rows.add(ResultEntryRow.builder().source(source).marksInput(marksInput).build());
        }
        return rows;
    }

    private static boolean containsId(List<Subject> subjects, String subjectId) {
        for (Subject subject : subjects) {
            if (subject.getId().equals(subjectId)) return true;
        }
        return false;
    }

    // ---- Sheet edits ----

    public void updateTotalMarks(String value) {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (state.isTotalMarksLocked()) return;
        if (!isValidDigits(value)) return;
        uiState.setValue(state.toBuilder().totalMarksInput(value).build());
    }

    public void updateMarks(String studentUid, String value) {
        if (!isValidDigits(value)) return;
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        List<ResultEntryRow> rows = new ArrayList<>();
        for (ResultEntryRow row : state.getRows()) {
            if (row.getSource().getStudent().getUid().equals(studentUid)) {
                rows.add(row.toBuilder().marksInput(value).build());
            } else {
                rows.add(row);
            }
        }
        uiState.setValue(state.toBuilder().rows(rows).build());
    }

    private boolean isValidDigits(String value) {
        if (value == null || value.length() > 4) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    // ---- Save / Submit ----

    public void saveDraft() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(state.getSelectedExamType())) {
            setActionMessage("Repeat exams are submitted directly for HOD approval.");
            return;
        }
        int total = state.getTotalMarksValue();
        List<StudentMarksDto> toSave = new ArrayList<>();
        // Empty input is expected (the teacher hasn't reached that student yet) and is skipped
        // silently. Non-empty but out-of-range input is a real mistake - it's skipped from the
        // save too (never silently accepted), but the teacher is told exactly who and why.
        List<String> invalidNames = new ArrayList<>();
        for (ResultEntryRow row : state.getRows()) {
            if (!row.isEditable(state.getSelectedExamType())) continue;
            String input = row.getMarksInput();
            if (input == null || input.trim().isEmpty()) continue;
            Integer marks = ResultEntryRow.parseIntOrNull(input);
            if (marks == null || marks < 0 || marks > total) {
                invalidNames.add(row.getSource().getStudent().getFullName());
                continue;
            }
            toSave.add(new StudentMarksDto(row.getSource().getStudent().getUid(), row.percentageToSubmit(total)));
        }
        if (toSave.isEmpty()) {
            setActionMessage(invalidNames.isEmpty()
                    ? "Enter marks for at least one student first."
                    : "Marks must be between 0 and " + total + ". Fix: " + String.join(", ", invalidNames));
            return;
        }
        uiState.setValue(state.toBuilder().saving(true).build());
        List<String> finalInvalidNames = invalidNames;
        examResultDataSource.saveDrafts(state.getSelectedSubjectId(), toSave)
                .addOnSuccessListener(results -> {
                    String message = finalInvalidNames.isEmpty()
                            ? "Draft saved."
                            : "Draft saved. Skipped " + finalInvalidNames.size()
                                    + (finalInvalidNames.size() == 1 ? " invalid entry" : " invalid entries")
                                    + " (marks must be 0-" + total + "): " + String.join(", ", finalInvalidNames);
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage(message).build());
                    loadRoster();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage(e.getMessage()).build()));
    }

    public void submit() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (!state.canSubmit()) {
            setActionMessage("Enter valid marks for every student before submitting.");
            return;
        }
        if (TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(state.getSelectedExamType())) {
            submitRepeat();
        } else {
            submitStandard(state);
        }
    }

    private void submitStandard(TeacherExamResultWorkspaceUiState state) {
        int total = state.getTotalMarksValue();
        List<StudentMarksDto> toSubmit = new ArrayList<>();
        for (ResultEntryRow row : state.getRows()) {
            if (!row.isEditable(state.getSelectedExamType())) continue;
            toSubmit.add(new StudentMarksDto(row.getSource().getStudent().getUid(), row.percentageToSubmit(total)));
        }
        if (toSubmit.isEmpty()) {
            setActionMessage("Enter marks for at least one student first.");
            return;
        }
        uiState.setValue(state.toBuilder().saving(true).build());
        examResultDataSource.submitForApproval(state.getSelectedSubjectId(), toSubmit)
                .addOnSuccessListener(results -> {
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage("Submitted for HOD approval.").build());
                    loadRoster();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage(e.getMessage()).build()));
    }

    /** Repeat exams: one submission per editable student - create the repeat exam on the
     * subject's FAIL result (when not already created), then push the marks for HOD approval. */
    private void submitRepeat() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        List<ResultEntryRow> targets = new ArrayList<>();
        for (ResultEntryRow row : state.getRows()) {
            if (row.isEditable(state.getSelectedExamType())) targets.add(row);
        }
        if (targets.isEmpty()) {
            setActionMessage("No repeat exams to submit.");
            return;
        }
        uiState.setValue(state.toBuilder().saving(true).build());
        submitNext(targets, 0, targets.size(), new ArrayList<>(), new int[]{0});
    }

    private void submitNext(List<ResultEntryRow> targets, int index, int total, List<String> failures, int[] success) {
        if (index >= total) {
            int ok = success[0];
            int failed = failures.size();
            String message;
            if (failed == 0) {
                message = ok + " repeat exam(s) submitted for HOD approval.";
            } else {
                message = ok + " submitted, " + failed + " failed: " + String.join(", ", failures);
            }
            uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage(message).build());
            loadRoster();
            return;
        }
        ResultEntryRow row = targets.get(index);
        int marks = row.repeatMarksToSubmit();
        submitOneRepeat(row, marks)
                .addOnSuccessListener(repeatExam -> {
                    success[0]++;
                    submitNext(targets, index + 1, total, failures, success);
                })
                .addOnFailureListener(e -> {
                    failures.add(row.getSource().getStudent().getFullName() + " (" + e.getMessage() + ")");
                    submitNext(targets, index + 1, total, failures, success);
                });
    }

    private Task<RepeatExam> submitOneRepeat(ResultEntryRow row, int marks) {
        RepeatExam repeat = row.getSource().getRepeatExam();
        if (repeat == null) {
            ExamResult result = row.getSource().getResult();
            if (result == null) return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("no result to attach repeat exam to"));
            return repeatExamDataSource.create(result.getId())
                    .continueWithTask(created -> repeatExamDataSource.submitMarks(created.getResult().getId(), marks));
        }
        return repeatExamDataSource.submitMarks(repeat.getId(), marks);
    }

    private void setActionMessage(String message) {
        uiState.setValue(uiState.getValue().toBuilder().actionMessage(message).build());
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().actionMessage(null).sectionError(null).build());
    }

    // ---- Manual refresh ----

    /** Re-fetches whatever this screen is currently showing - the roster if a subject/exam-type
     * is selected, the subject list if only the semester is picked, or the top-level department/
     * session/semester options otherwise. No-ops while any fetch is already in flight. */
    public void refresh() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        if (isBusy() || state.isSaving()) return;
        if (state.isShowCourseGrid()) {
            loadCourseOverview();
        } else if (state.getSelectedSubjectId() != null) {
            loadRoster();
        } else if (state.getSelectedSemesterId() != null) {
            loadSubjects();
        } else {
            loadOptions();
        }
    }

    public boolean isBusy() {
        TeacherExamResultWorkspaceUiState state = uiState.getValue();
        return state.isLoadingOptions() || state.isLoadingSubjects() || state.isLoadingRoster() || state.isLoadingCourses();
    }
}
