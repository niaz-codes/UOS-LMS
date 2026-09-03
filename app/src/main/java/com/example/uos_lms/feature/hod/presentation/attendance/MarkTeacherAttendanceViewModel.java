package com.example.uos_lms.feature.hod.presentation.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.data.remote.api.ApiTeacherAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.dto.SubjectStatusDto;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.TeacherAttendanceRecord;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MarkTeacherAttendanceViewModel extends ViewModel {

    private final ApiTeacherAttendanceDataSource teacherAttendanceDataSource;
    private final String hodUid;
    private final String departmentId;
    private final String semesterId;
    private final String dateKey;

    private final MutableLiveData<MarkTeacherAttendanceUiState> uiState = new MutableLiveData<>(MarkTeacherAttendanceUiState.initial());
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);

    @Inject
    public MarkTeacherAttendanceViewModel(
            SavedStateHandle savedStateHandle,
            ApiTeacherAttendanceDataSource teacherAttendanceDataSource,
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            SessionManager sessionManager) {
        this.teacherAttendanceDataSource = teacherAttendanceDataSource;
        CachedSession session = sessionManager.getCachedSession().getValue();
        this.hodUid = session != null ? session.getUid() : null;
        this.departmentId = savedStateHandle.get("departmentId");
        this.semesterId = savedStateHandle.get("semesterId");
        this.dateKey = savedStateHandle.get("dateKey");
        uiState.setValue(uiState.getValue().toBuilder().dateLabel(DateKeyUtils.dateKeyToDisplay(dateKey)).build());
        load(universityDataSource, userDataSource);
    }

    public LiveData<MarkTeacherAttendanceUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    private void load(ApiUniversityDataSource universityDataSource, ApiUserDataSource userDataSource) {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());

        universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> {
            for (Semester semester : semesters) {
                if (semester.getId().equals(semesterId)) {
                    uiState.setValue(uiState.getValue().toBuilder().semesterLabel(semester.getDisplayName()).build());
                    break;
                }
            }
        });

        Tasks.whenAllSuccess(
                universityDataSource.listSubjectsForDepartment(departmentId),
                teacherAttendanceDataSource.forSemesterAndDate(semesterId, dateKey),
                userDataSource.listApprovedTeachersInDepartment(departmentId)
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            List<Subject> subjects = (List<Subject>) results.get(0);
            //noinspection unchecked
            List<TeacherAttendanceRecord> records = (List<TeacherAttendanceRecord>) results.get(1);
            //noinspection unchecked
            List<User> teachers = (List<User>) results.get(2);
            recombine(subjects, records, teachers);
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    /** Subject.getTeacherName() is always blank from listSubjectsForDepartment (the endpoint
     * only returns the raw teacherId, see SubjectResponseDto) - names are resolved here from
     * the department's teacher roster and stitched onto each subject before display. */
    private void recombine(List<Subject> subjects, List<TeacherAttendanceRecord> records, List<User> teachers) {
        Map<String, String> nameByTeacherUid = new HashMap<>();
        for (User teacher : teachers) nameByTeacherUid.put(teacher.getUid(), teacher.getFullName());

        List<Subject> inSemester = new ArrayList<>();
        for (Subject subject : subjects) {
            if (semesterId.equals(subject.getSemesterId()) && subject.getTeacherUid() != null) {
                String teacherName = nameByTeacherUid.get(subject.getTeacherUid());
                inSemester.add(subject.toBuilder().teacherName(teacherName != null ? teacherName : "").build());
            }
        }
        inSemester.sort((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()));

        List<SubjectAttendanceRow> rows = new ArrayList<>();
        for (Subject subject : inSemester) {
            AttendanceStatus status = AttendanceStatus.PRESENT;
            for (TeacherAttendanceRecord record : records) {
                if (record.getSubjectId().equals(subject.getId())) {
                    status = record.getStatus();
                    break;
                }
            }
            rows.add(SubjectAttendanceRow.builder().subject(subject).status(status).build());
        }
        uiState.setValue(uiState.getValue().toBuilder().rows(rows).loading(false).build());
    }

    public void toggleStatus(String subjectId) {
        List<SubjectAttendanceRow> rows = new ArrayList<>();
        for (SubjectAttendanceRow row : uiState.getValue().getRows()) {
            if (row.getSubject().getId().equals(subjectId)) {
                AttendanceStatus next = row.getStatus() == AttendanceStatus.PRESENT ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT;
                rows.add(row.toBuilder().status(next).build());
            } else {
                rows.add(row);
            }
        }
        uiState.setValue(uiState.getValue().toBuilder().rows(rows).build());
    }

    public void save() {
        if (hodUid == null) {
            uiState.setValue(uiState.getValue().toBuilder().errorMessage("Session expired. Please log in again.").build());
            return;
        }
        List<SubjectStatusDto> records = new ArrayList<>();
        for (SubjectAttendanceRow row : uiState.getValue().getRows()) {
            records.add(new SubjectStatusDto(row.getSubject().getId(), row.getStatus().name()));
        }
        uiState.setValue(uiState.getValue().toBuilder().saving(true).errorMessage(null).build());
        teacherAttendanceDataSource.saveAttendance(departmentId, semesterId, dateKey, records)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).build());
                    saved.setValue(true);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().saving(false).errorMessage(e.getMessage()).build()));
    }
}
