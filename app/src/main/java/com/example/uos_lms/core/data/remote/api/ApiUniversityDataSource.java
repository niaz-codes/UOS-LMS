package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AssignTeacherRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateDepartmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateSemesterRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateSessionRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateSubjectRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.DepartmentResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.ReassignSessionStudentsRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.SemesterResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SessionResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SubjectResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SubjectsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherStudentRowDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherSubjectSummaryDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateDepartmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateSessionRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateSubjectRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UserResponseDto;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.TeacherStudentRoster;
import com.example.uos_lms.core.domain.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for the department/session/semester/subject-roster parts of
 * FirestoreUniversityDataSource that the grading/promotion screens need. One-shot only. */
@Singleton
public class ApiUniversityDataSource {

    private final UniversityApi universityApi;

    @Inject
    public ApiUniversityDataSource(UniversityApi universityApi) {
        this.universityApi = universityApi;
    }

    public Task<List<Department>> listDepartments() {
        return RetrofitTasks.call(universityApi.listDepartments()).onSuccessTask(envelope -> {
            List<Department> departments = new ArrayList<>();
            for (DepartmentResponseDto dto : envelope.getDepartments()) departments.add(dto.toDomain());
            return Tasks.forResult(departments);
        });
    }

    public Task<List<Session>> listSessions(String departmentId) {
        return RetrofitTasks.call(universityApi.listSessions(departmentId)).onSuccessTask(envelope -> {
            List<Session> sessions = new ArrayList<>();
            for (SessionResponseDto dto : envelope.getSessions()) sessions.add(dto.toDomain());
            return Tasks.forResult(sessions);
        });
    }

    public Task<List<Semester>> listSemesters(String departmentId) {
        return RetrofitTasks.call(universityApi.listSemesters(departmentId)).onSuccessTask(envelope -> {
            List<Semester> semesters = new ArrayList<>();
            for (SemesterResponseDto dto : envelope.getSemesters()) semesters.add(dto.toDomain());
            return Tasks.forResult(semesters);
        });
    }

    public Task<List<Subject>> listSubjectsForDepartment(String departmentId) {
        return RetrofitTasks.call(universityApi.listSubjectsForDepartment(departmentId)).onSuccessTask(this::toSubjectList);
    }

    public Task<List<Subject>> listSubjectsForSemester(String semesterId) {
        return RetrofitTasks.call(universityApi.listSubjectsForSemester(semesterId)).onSuccessTask(this::toSubjectList);
    }

    public Task<List<Subject>> listSubjectsForTeacher(String teacherId) {
        return RetrofitTasks.call(universityApi.listSubjectsForTeacher(teacherId)).onSuccessTask(this::toSubjectList);
    }

    /** Student's own subjects: current department+semester unioned with retakeSubjectIds. */
    public Task<List<Subject>> mySubjects() {
        return RetrofitTasks.call(universityApi.mySubjects()).onSuccessTask(this::toSubjectList);
    }

    private Task<List<Subject>> toSubjectList(SubjectsEnvelopeDto envelope) {
        List<Subject> subjects = new ArrayList<>();
        if (envelope.getSubjects() != null) {
            for (SubjectResponseDto dto : envelope.getSubjects()) subjects.add(dto.toDomain());
        }
        return Tasks.forResult(subjects);
    }

    public Task<List<User>> subjectRoster(String subjectId) {
        return RetrofitTasks.call(universityApi.subjectRoster(subjectId)).onSuccessTask(envelope -> {
            List<User> students = new ArrayList<>();
            if (envelope.getStudents() != null) {
                for (UserResponseDto dto : envelope.getStudents()) students.add(dto.toDomain());
            }
            return Tasks.forResult(students);
        });
    }

    /** The authoritative, already-deduplicated union of every student enrolled in any subject
     * assigned to the currently logged-in teacher - see subjectController.myStudents. */
    public Task<TeacherStudentRoster> myStudents() {
        return RetrofitTasks.call(universityApi.myStudents()).onSuccessTask(envelope -> {
            List<TeacherStudentRoster.SubjectRoster> subjects = new ArrayList<>();
            if (envelope.getSubjects() != null) {
                for (TeacherSubjectSummaryDto dto : envelope.getSubjects()) {
                    subjects.add(TeacherStudentRoster.SubjectRoster.builder()
                            .subject(dto.toDomain())
                            .studentCount(dto.getStudentCount())
                            .build());
                }
            }
            List<TeacherStudentRoster.StudentEnrollment> students = new ArrayList<>();
            if (envelope.getStudents() != null) {
                for (TeacherStudentRowDto dto : envelope.getStudents()) {
                    students.add(TeacherStudentRoster.StudentEnrollment.builder()
                            .user(dto.toDomain())
                            .subjectIds(dto.getSubjectIds())
                            .build());
                }
            }
            return Tasks.forResult(TeacherStudentRoster.builder().subjects(subjects).students(students).build());
        });
    }

    // ---- Department CRUD (Admin) ----

    public Task<Department> createDepartment(String name, String code, String description) {
        CreateDepartmentRequestDto request = CreateDepartmentRequestDto.builder().name(name).code(code).description(description).build();
        return RetrofitTasks.call(universityApi.createDepartment(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getDepartment().toDomain()));
    }

    public Task<Department> updateDepartment(String id, String name, String code, String description) {
        UpdateDepartmentRequestDto request = UpdateDepartmentRequestDto.builder().name(name).code(code).description(description).build();
        return RetrofitTasks.call(universityApi.updateDepartment(id, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getDepartment().toDomain()));
    }

    public Task<Void> deleteDepartment(String id) {
        return RetrofitTasks.call(universityApi.removeDepartment(id));
    }

    // ---- Session CRUD (Admin) ----

    public Task<Session> createSession(String departmentId, String label) {
        CreateSessionRequestDto request = CreateSessionRequestDto.builder().label(label).build();
        return RetrofitTasks.call(universityApi.createSession(departmentId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSession().toDomain()));
    }

    public Task<Session> updateSession(String id, String label, Boolean isActive) {
        UpdateSessionRequestDto request = UpdateSessionRequestDto.builder().label(label).isActive(isActive).build();
        return RetrofitTasks.call(universityApi.updateSession(id, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSession().toDomain()));
    }

    public Task<Void> reassignSessionStudents(String sessionId, String toSessionId) {
        ReassignSessionStudentsRequestDto request = ReassignSessionStudentsRequestDto.builder().toSessionId(toSessionId).build();
        return RetrofitTasks.call(universityApi.reassignSessionStudents(sessionId, request));
    }

    public Task<Void> deleteSession(String id) {
        return RetrofitTasks.call(universityApi.removeSession(id));
    }

    public Task<Integer> countAllSessions() {
        return RetrofitTasks.call(universityApi.countAllSessions()).onSuccessTask(envelope -> Tasks.forResult(envelope.getCount()));
    }

    // ---- Semester CRUD (Admin) ----

    public Task<Semester> createSemester(String departmentId, int number) {
        CreateSemesterRequestDto request = CreateSemesterRequestDto.builder().number(number).build();
        return RetrofitTasks.call(universityApi.createSemester(departmentId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSemester().toDomain()));
    }

    public Task<Void> deleteSemester(String id) {
        return RetrofitTasks.call(universityApi.removeSemester(id));
    }

    public Task<Integer> countAllSemesters() {
        return RetrofitTasks.call(universityApi.countAllSemesters()).onSuccessTask(envelope -> Tasks.forResult(envelope.getCount()));
    }

    // ---- Subject CRUD (Admin) + teacher assignment (Admin/HOD) ----

    public Task<Subject> createSubject(String departmentId, String semesterId, String code, String title, int creditHours) {
        CreateSubjectRequestDto request = CreateSubjectRequestDto.builder()
                .departmentId(departmentId).semesterId(semesterId).code(code).title(title).creditHours(creditHours).build();
        return RetrofitTasks.call(universityApi.createSubject(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSubject().toDomain()));
    }

    public Task<Subject> updateSubject(String id, String code, String title, Integer creditHours) {
        UpdateSubjectRequestDto request = UpdateSubjectRequestDto.builder().code(code).title(title).creditHours(creditHours).build();
        return RetrofitTasks.call(universityApi.updateSubject(id, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSubject().toDomain()));
    }

    public Task<Void> deleteSubject(String id) {
        return RetrofitTasks.call(universityApi.removeSubject(id));
    }

    public Task<Subject> assignTeacher(String subjectId, String teacherId) {
        AssignTeacherRequestDto request = AssignTeacherRequestDto.builder().teacherId(teacherId).unassign(false).build();
        return RetrofitTasks.call(universityApi.assignTeacher(subjectId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSubject().toDomain()));
    }

    public Task<Subject> unassignTeacher(String subjectId) {
        AssignTeacherRequestDto request = AssignTeacherRequestDto.builder().unassign(true).build();
        return RetrofitTasks.call(universityApi.assignTeacher(subjectId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSubject().toDomain()));
    }
}
