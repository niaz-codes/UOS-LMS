package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.DepartmentResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SemesterResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SessionResponseDto;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Backs the registration screen's Department / Session / Semester option pickers through the
 * public (no-token) endpoints - see {@link PublicApi}. One-shot reads only. */
@Singleton
public class RegistrationOptionsDataSource {

    private final PublicApi publicApi;

    @Inject
    public RegistrationOptionsDataSource(PublicApi publicApi) {
        this.publicApi = publicApi;
    }

    public Task<List<Department>> listDepartments() {
        return RetrofitTasks.call(publicApi.listDepartments()).onSuccessTask(envelope -> {
            List<Department> departments = new ArrayList<>();
            if (envelope.getDepartments() != null) {
                for (DepartmentResponseDto dto : envelope.getDepartments()) departments.add(dto.toDomain());
            }
            return Tasks.forResult(departments);
        });
    }

    public Task<List<Session>> listSessions(String departmentId) {
        return RetrofitTasks.call(publicApi.listSessions(departmentId)).onSuccessTask(envelope -> {
            List<Session> sessions = new ArrayList<>();
            if (envelope.getSessions() != null) {
                for (SessionResponseDto dto : envelope.getSessions()) sessions.add(dto.toDomain());
            }
            return Tasks.forResult(sessions);
        });
    }

    public Task<List<Semester>> listSemesters(String departmentId) {
        return RetrofitTasks.call(publicApi.listSemesters(departmentId)).onSuccessTask(envelope -> {
            List<Semester> semesters = new ArrayList<>();
            if (envelope.getSemesters() != null) {
                for (SemesterResponseDto dto : envelope.getSemesters()) semesters.add(dto.toDomain());
            }
            return Tasks.forResult(semesters);
        });
    }
}
