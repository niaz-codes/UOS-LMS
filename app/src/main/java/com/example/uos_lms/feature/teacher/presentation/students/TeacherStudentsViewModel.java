package com.example.uos_lms.feature.teacher.presentation.students;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.TeacherStudentRoster;
import com.example.uos_lms.core.domain.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** The Teacher's own student roster: the union of every subject actually assigned to the
 * currently logged-in teacher, deduplicated by student id - never a department- or
 * university-wide list. All authorization and deduplication happens server-side
 * (subjectController.myStudents, keyed off the authenticated req.user, never a client-sent
 * id) - this ViewModel only resolves display names (department/session/semester) for the
 * subjects and students the backend already decided this teacher is allowed to see. */
@HiltViewModel
public class TeacherStudentsViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<TeacherStudentsUiState> uiState = new MutableLiveData<>(TeacherStudentsUiState.initial());

    private final Map<String, String> departmentNamesById = new HashMap<>();
    private final Map<String, List<Semester>> semestersByDept = new HashMap<>();
    private final Map<String, String> sessionLabelsById = new HashMap<>();

    @Inject
    public TeacherStudentsViewModel(ApiUniversityDataSource universityDataSource) {
        this.universityDataSource = universityDataSource;
        load();
    }

    public LiveData<TeacherStudentsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches the roster from the backend without blanking the currently-shown list,
     * preserving the search/subject filter - no-ops while a refresh is already in flight. */
    public void refresh() {
        TeacherStudentsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        universityDataSource.myStudents()
                .addOnSuccessListener(this::loadMetadataThenCombine)
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    /** Fetches department names and each department's semesters/sessions - scoped to just the
     * departments the teacher's assigned subjects actually span, mirroring
     * TeacherDashboardViewModel's identical metadata-resolution pattern. */
    private void loadMetadataThenCombine(TeacherStudentRoster roster) {
        List<Subject> subjects = new ArrayList<>();
        for (TeacherStudentRoster.SubjectRoster subjectRoster : roster.getSubjects()) subjects.add(subjectRoster.getSubject());

        if (subjects.isEmpty()) {
            uiState.setValue(uiState.getValue().toBuilder()
                    .subjects(subjects).allStudents(new ArrayList<>()).loading(false).refreshing(false).build());
            return;
        }

        Set<String> departmentIds = new HashSet<>();
        for (Subject subject : subjects) departmentIds.add(subject.getDepartmentId());

        List<Task<?>> tasks = new ArrayList<>();
        tasks.add(universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            for (Department department : departments) {
                if (departmentIds.contains(department.getId())) departmentNamesById.put(department.getId(), department.getName());
            }
        }));
        for (String departmentId : departmentIds) {
            tasks.add(universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> semestersByDept.put(departmentId, semesters)));
            tasks.add(universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions -> {
                for (Session session : sessions) sessionLabelsById.put(session.getId(), session.getLabel());
            }));
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> combine(subjects, roster.getStudents()));
    }

    private void combine(List<Subject> subjects, List<TeacherStudentRoster.StudentEnrollment> enrollments) {
        Map<String, Subject> subjectsById = new HashMap<>();
        for (Subject subject : subjects) subjectsById.put(subject.getId(), subject);

        List<TeacherStudentSummary> summaries = new ArrayList<>();
        for (TeacherStudentRoster.StudentEnrollment enrollment : enrollments) {
            User user = enrollment.getUser();
            List<Subject> courses = new ArrayList<>();
            for (String subjectId : enrollment.getSubjectIds()) {
                Subject subject = subjectsById.get(subjectId);
                if (subject != null) courses.add(subject);
            }
            courses.sort((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()));

            summaries.add(TeacherStudentSummary.builder()
                    .user(user)
                    .subjectIds(enrollment.getSubjectIds())
                    .courses(courses)
                    .departmentName(departmentNamesById.getOrDefault(user.getDepartment(), null))
                    .sessionLabel(sessionLabelsById.getOrDefault(user.getSessionId(), null))
                    .semesterLabel(resolveSemesterLabel(user))
                    .build());
        }

        uiState.setValue(uiState.getValue().toBuilder()
                .subjects(subjects).allStudents(summaries).loading(false).refreshing(false).build());
    }

    private String resolveSemesterLabel(User user) {
        if (user.getDepartment() == null || user.getSemester() == null) return null;
        List<Semester> semesters = semestersByDept.get(user.getDepartment());
        if (semesters == null) return null;
        for (Semester semester : semesters) {
            if (semester.getId().equals(user.getSemester())) return semester.getDisplayName();
        }
        return null;
    }

    public void onSearchQueryChange(String query) {
        uiState.setValue(uiState.getValue().toBuilder().searchQuery(query).build());
    }

    /** Filters to one of the teacher's own subjects (or null for "all") - never an arbitrary
     * department/semester, so this can never widen the roster beyond what the backend already
     * scoped to this teacher. */
    public void onSubjectSelected(Subject subject) {
        uiState.setValue(uiState.getValue().toBuilder().selectedSubject(subject).build());
    }
}
