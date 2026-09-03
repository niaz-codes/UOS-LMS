package com.example.uos_lms.core.data.remote.api;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.data.remote.api.dto.UpdateAcademicPlacementRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserDepartmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserIdentifiersRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserProfileRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserRoleRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserStatusRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UserResponseDto;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed user search, Admin or HOD only (see backend users.js RBAC; HOD is force-scoped
 * to their own department server-side) - used where a screen needs a filtered roster rather
 * than a single lookup (AuthApi.me() covers "my own profile" for every role already). */
@Singleton
public class ApiUserDataSource {

    private final UsersApi usersApi;

    @Inject
    public ApiUserDataSource(UsersApi usersApi) {
        this.usersApi = usersApi;
    }

    public Task<User> getUser(String uid) {
        return RetrofitTasks.call(usersApi.getById(uid)).onSuccessTask(envelope ->
                Tasks.forResult(envelope.getUser() != null ? envelope.getUser().toDomain() : null));
    }

    public Task<List<User>> listStudentsInSession(String departmentId, String sessionId, String semesterId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("role", "STUDENT");
        filters.put("departmentId", departmentId);
        filters.put("sessionId", sessionId);
        filters.put("currentSemesterId", semesterId);
        return list(filters);
    }

    public Task<List<User>> listStudentsInSemester(String departmentId, String semesterId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("role", "STUDENT");
        filters.put("departmentId", departmentId);
        filters.put("currentSemesterId", semesterId);
        return list(filters);
    }

    /** Every role, one department - the flat roster behind a department's own detail screen. */
    public Task<List<User>> listUsersInDepartment(String departmentId) {
        return list(singleFilter("departmentId", departmentId));
    }

    public Task<List<User>> listUsersInDepartmentByRole(String departmentId, UserRole role) {
        Map<String, String> filters = new HashMap<>();
        filters.put("departmentId", departmentId);
        filters.put("role", role.name());
        return list(filters);
    }

    public Task<List<User>> listApprovedTeachersInDepartment(String departmentId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("role", "TEACHER");
        filters.put("status", "APPROVED");
        filters.put("departmentId", departmentId);
        return list(filters);
    }

    /** Self-scoped: everyone this caller may start a conversation with, per the app-wide
     * messaging role-pair policy - see backend userController.contacts. */
    public Task<List<User>> messagingContacts() {
        return RetrofitTasks.call(usersApi.messagingContacts()).onSuccessTask(envelope -> {
            List<User> users = new ArrayList<>();
            if (envelope.getUsers() != null) {
                for (UserResponseDto dto : envelope.getUsers()) users.add(dto.toDomain());
            }
            return Tasks.forResult(users);
        });
    }

    /** Admin-only: accounts awaiting approval - consumed by AppNotificationCenter. */
    public Task<List<User>> listPendingUsers() {
        Map<String, String> filters = new HashMap<>();
        filters.put("status", "PENDING");
        return list(filters);
    }

    public Task<List<User>> listApprovedTeachers() {
        Map<String, String> filters = new HashMap<>();
        filters.put("role", "TEACHER");
        filters.put("status", "APPROVED");
        return list(filters);
    }

    /** Admin: every user in the system. HOD: every user in their own department (the backend
     * force-scopes the HOD case server-side, see userController.list). */
    public Task<List<User>> allUsers() {
        return list(new HashMap<>());
    }

    private Task<List<User>> list(Map<String, String> filters) {
        return RetrofitTasks.call(usersApi.list(filters)).onSuccessTask(envelope -> {
            List<User> users = new ArrayList<>();
            if (envelope.getUsers() != null) {
                for (UserResponseDto dto : envelope.getUsers()) users.add(dto.toDomain());
            }
            return Tasks.forResult(users);
        });
    }

    // ---- Aggregate counts (GET /users/counts) ----

    public Task<Integer> countAllUsers() {
        return count(new HashMap<>());
    }

    public Task<Integer> countByRole(UserRole role) {
        return count(singleFilter("role", role.name()));
    }

    public Task<Integer> countInDepartmentByRole(String departmentId, UserRole role) {
        Map<String, String> filters = new HashMap<>();
        filters.put("departmentId", departmentId);
        filters.put("role", role.name());
        return count(filters);
    }

    public Task<Integer> countStudentsInSession(String departmentId, String sessionId, String semesterId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("role", "STUDENT");
        filters.put("departmentId", departmentId);
        if (sessionId != null) filters.put("sessionId", sessionId);
        if (semesterId != null) filters.put("semesterId", semesterId);
        return count(filters);
    }

    public Task<Integer> countStudentsBySessionId(String sessionId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("role", "STUDENT");
        filters.put("sessionId", sessionId);
        return count(filters);
    }

    private Task<Integer> count(Map<String, String> filters) {
        return RetrofitTasks.call(usersApi.counts(filters)).onSuccessTask(envelope -> Tasks.forResult(envelope.getCount()));
    }

    // ---- Admin user-management mutations ----

    public Task<User> updateStatus(String uid, UserStatus status, @Nullable String reason) {
        UpdateUserStatusRequestDto request = UpdateUserStatusRequestDto.builder().status(status.name()).reason(reason).build();
        return RetrofitTasks.call(usersApi.updateStatus(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<Void> deleteUser(String uid) {
        return RetrofitTasks.call(usersApi.remove(uid));
    }

    /** HOD/Student - single department. */
    public Task<User> updateUserDepartment(String uid, String departmentId) {
        UpdateUserDepartmentRequestDto request = UpdateUserDepartmentRequestDto.builder().departmentId(departmentId).build();
        return RetrofitTasks.call(usersApi.updateDepartment(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> unassignUserDepartment(String uid) {
        UpdateUserDepartmentRequestDto request = UpdateUserDepartmentRequestDto.builder().unassign(true).build();
        return RetrofitTasks.call(usersApi.updateDepartment(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    /** Teacher - multi-department. */
    public Task<User> updateUserDepartments(String uid, List<String> departmentIds) {
        UpdateUserDepartmentRequestDto request = UpdateUserDepartmentRequestDto.builder()
                .departmentIds(departmentIds != null ? departmentIds : Collections.emptyList()).build();
        return RetrofitTasks.call(usersApi.updateDepartment(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> assignSemester(String uid, String semesterId) {
        UpdateAcademicPlacementRequestDto request = UpdateAcademicPlacementRequestDto.builder().currentSemesterId(semesterId).build();
        return RetrofitTasks.call(usersApi.updateAcademicPlacement(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> unassignSemester(String uid) {
        UpdateAcademicPlacementRequestDto request = UpdateAcademicPlacementRequestDto.builder().clearSemester(true).build();
        return RetrofitTasks.call(usersApi.updateAcademicPlacement(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> assignSession(String uid, String sessionId) {
        UpdateAcademicPlacementRequestDto request = UpdateAcademicPlacementRequestDto.builder().sessionId(sessionId).build();
        return RetrofitTasks.call(usersApi.updateAcademicPlacement(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> unassignSession(String uid) {
        UpdateAcademicPlacementRequestDto request = UpdateAcademicPlacementRequestDto.builder().clearSession(true).build();
        return RetrofitTasks.call(usersApi.updateAcademicPlacement(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> updateProfile(String uid, String fullName, String fatherName, String phone, String cnic) {
        UpdateUserProfileRequestDto request = UpdateUserProfileRequestDto.builder()
                .fullName(fullName).fatherName(fatherName).phone(phone).cnic(cnic).build();
        return RetrofitTasks.call(usersApi.updateProfile(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> updateIdentifiers(String uid, String employeeId, String designation, String registrationNumber, String rollNumber) {
        UpdateUserIdentifiersRequestDto request = UpdateUserIdentifiersRequestDto.builder()
                .employeeId(employeeId).designation(designation).registrationNumber(registrationNumber).rollNumber(rollNumber).build();
        return RetrofitTasks.call(usersApi.updateIdentifiers(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    public Task<User> updateRole(String uid, UserRole role) {
        UpdateUserRoleRequestDto request = UpdateUserRoleRequestDto.builder().role(role.name()).build();
        return RetrofitTasks.call(usersApi.updateRole(uid, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()));
    }

    /** The one existing APPROVED HOD of a department, if any - null if the department is
     * currently unled. Used to warn the Admin before reassigning a department to a
     * different HOD (they'd be silently displacing someone). */
    public Task<User> findHodForDepartment(String departmentId) {
        return RetrofitTasks.call(usersApi.hodForDepartment(departmentId)).onSuccessTask(envelope ->
                Tasks.forResult(envelope.getUser() != null ? envelope.getUser().toDomain() : null));
    }

    private static Map<String, String> singleFilter(String key, String value) {
        Map<String, String> filters = new HashMap<>();
        filters.put(key, value);
        return filters;
    }
}
