package com.example.uos_lms.feature.auth.presentation.register;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.SingleLiveEvent;
import com.example.uos_lms.core.data.remote.api.RegistrationOptionsDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.validation.CnicValidator;
import com.example.uos_lms.core.validation.EmailValidator;
import com.example.uos_lms.core.validation.NameValidator;
import com.example.uos_lms.core.validation.PasswordValidator;
import com.example.uos_lms.core.validation.PhoneValidator;
import com.example.uos_lms.core.validation.ValidationResult;
import com.example.uos_lms.feature.auth.data.AuthDataSource;
import com.example.uos_lms.feature.auth.data.RegisterSelections;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class RegisterViewModel extends ViewModel {

    private final Context appContext;
    private final AuthDataSource authDataSource;
    private final RegistrationOptionsDataSource optionsDataSource;

    private final MutableLiveData<RegisterUiState> uiState = new MutableLiveData<>(RegisterUiState.initial());
    private final SingleLiveEvent<User> registrationSubmitted = new SingleLiveEvent<>();

    @Inject
    public RegisterViewModel(@ApplicationContext Context appContext,
                             AuthDataSource authDataSource,
                             RegistrationOptionsDataSource optionsDataSource) {
        this.appContext = appContext;
        this.authDataSource = authDataSource;
        this.optionsDataSource = optionsDataSource;
        loadDepartments();
    }

    public LiveData<RegisterUiState> getUiState() {
        return uiState;
    }

    public LiveData<User> getRegistrationSubmitted() {
        return registrationSubmitted;
    }

    private RegisterUiState state() {
        return uiState.getValue();
    }

    // ---- Departments (all roles) ----

    private void loadDepartments() {
        uiState.setValue(state().toBuilder().departmentsLoading(true).departmentsError(null).build());
        optionsDataSource.listDepartments().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uiState.setValue(state().toBuilder()
                        .departmentsLoading(false)
                        .departments(task.getResult() != null ? task.getResult() : Collections.<Department>emptyList())
                        .build());
            } else {
                uiState.setValue(state().toBuilder()
                        .departmentsLoading(false)
                        .departmentsError(errorMessage(task.getException()))
                        .build());
            }
        });
    }

    public void retryLoadDepartments() {
        loadDepartments();
    }

    public void retryLoadStudentSelection() {
        String departmentId = state().getStudentDepartmentId();
        if (isBlank(departmentId)) return;
        loadStudentSelection(departmentId);
    }

    // ---- Form field changes ----

    public void onFullNameChange(String v) {
        uiState.setValue(state().toBuilder().fullName(v).fullNameError(null).build());
    }

    public void onFatherNameChange(String v) {
        uiState.setValue(state().toBuilder().fatherName(v).fatherNameError(null).build());
    }

    public void onCnicChange(String v) {
        uiState.setValue(state().toBuilder().cnic(v).cnicError(null).build());
    }

    public void onPhoneChange(String v) {
        uiState.setValue(state().toBuilder().phone(v).phoneError(null).build());
    }

    public void onEmailChange(String v) {
        uiState.setValue(state().toBuilder().email(v).emailError(null).build());
    }

    public void onPasswordChange(String v) {
        uiState.setValue(state().toBuilder().password(v).passwordError(null).build());
    }

    public void onConfirmPasswordChange(String v) {
        uiState.setValue(state().toBuilder().confirmPassword(v).confirmPasswordError(null).build());
    }

    public void onRoleChange(UserRole role) {
        if (role == state().getRole()) return;
        uiState.setValue(state().toBuilder().role(role).build());
    }

    public void onPhotoPicked(Uri uri) {
        uiState.setValue(state().toBuilder().photoUri(uri).build());
    }

    public void onPhotoRemoved() {
        uiState.setValue(state().toBuilder().photoUri(null).build());
    }

    // ---- Academic selections ----

    public void onStudentDepartmentSelected(String id) {
        RegisterUiState s = state();
        if (Objects.equals(id, s.getStudentDepartmentId())) return;
        uiState.setValue(s.toBuilder()
                .studentDepartmentId(id)
                .studentSessionId(null)
                .studentSemesterId(null)
                .studentSessions(Collections.<Session>emptyList())
                .studentSemesters(Collections.<Semester>emptyList())
                .departmentError(null)
                .sessionError(null)
                .semesterError(null)
                .selectionError(null)
                .build());
        if (id != null && !id.isEmpty()) loadStudentSelection(id);
    }

    public void onStudentSessionSelected(String id) {
        RegisterUiState s = state();
        if (Objects.equals(id, s.getStudentSessionId())) return;
        uiState.setValue(s.toBuilder().studentSessionId(id).sessionError(null).build());
    }

    public void onStudentSemesterSelected(String id) {
        RegisterUiState s = state();
        if (Objects.equals(id, s.getStudentSemesterId())) return;
        uiState.setValue(s.toBuilder().studentSemesterId(id).semesterError(null).build());
    }

    public void onTeacherDepartmentsSelected(List<String> ids) {
        List<String> normalized = ids == null
                ? Collections.<String>emptyList()
                : new ArrayList<>(new LinkedHashSet<>(ids));
        RegisterUiState s = state();
        if (normalized.equals(s.getTeacherDepartmentIds())) return;
        uiState.setValue(s.toBuilder()
                .teacherDepartmentIds(normalized)
                .departmentError(null)
                .build());
    }

    public void onHodDepartmentSelected(String id) {
        RegisterUiState s = state();
        if (Objects.equals(id, s.getHodDepartmentId())) return;
        uiState.setValue(s.toBuilder().hodDepartmentId(id).departmentError(null).build());
    }

    private void loadStudentSelection(String departmentId) {
        uiState.setValue(state().toBuilder().selectionLoading(true).selectionError(null).build());
        List<Task<?>> tasks = new ArrayList<>();
        tasks.add(optionsDataSource.listSessions(departmentId));
        tasks.add(optionsDataSource.listSemesters(departmentId));
        Tasks.whenAllSuccess(tasks).addOnCompleteListener(task -> {
            RegisterUiState s = state();
            if (!departmentId.equals(s.getStudentDepartmentId())) return; // stale response
            if (!task.isSuccessful()) {
                uiState.setValue(s.toBuilder()
                        .selectionLoading(false)
                        .selectionError(errorMessage(task.getException()))
                        .build());
                return;
            }
            List<Session> sessions = new ArrayList<>();
            List<Semester> semesters = new ArrayList<>();
            // Tasks.whenAllSuccess() already unwraps each task to its result value (a List<Session>
            // and a List<Semester> here) - task.getResult() is a List<Object> of those values
            // directly, NOT a list of Task objects, so no further unwrapping is needed. Casting
            // these entries back to Task and calling .getResult() again threw a
            // ClassCastException every time a department was picked - that was the crash.
            List<Object> unwrapped = task.getResult();
            Object sessionsResult = unwrapped.get(0);
            Object semestersResult = unwrapped.get(1);
            if (sessionsResult instanceof List) {
                for (Object item : (List<?>) sessionsResult) {
                    if (item instanceof Session && ((Session) item).isActive()) sessions.add((Session) item);
                }
            }
            if (semestersResult instanceof List) {
                for (Object item : (List<?>) semestersResult) {
                    if (item instanceof Semester) semesters.add((Semester) item);
                }
            }
            uiState.setValue(s.toBuilder()
                    .selectionLoading(false)
                    .studentSessions(sessions)
                    .studentSemesters(semesters)
                    .build());
        });
    }

    // ---- Submit ----

    public void submit() {
        RegisterUiState current = state();

        ValidationResult fullNameResult = NameValidator.validate(current.getFullName(), "Full name");
        ValidationResult fatherNameResult = NameValidator.validate(current.getFatherName(), "Father name");
        ValidationResult cnicResult = CnicValidator.validate(current.getCnic());
        ValidationResult phoneResult = PhoneValidator.validate(current.getPhone());
        ValidationResult emailResult = EmailValidator.validate(current.getEmail());
        ValidationResult passwordResult = PasswordValidator.validate(current.getPassword());
        ValidationResult confirmResult = PasswordValidator.validateConfirmation(current.getPassword(), current.getConfirmPassword());

        String departmentError = validateDepartmentSelection(current);
        String sessionError = null;
        String semesterError = null;
        if (current.getRole() == UserRole.STUDENT) {
            if (isBlank(current.getStudentSessionId())) {
                sessionError = appContext.getString(R.string.error_select_session);
            }
            if (isBlank(current.getStudentSemesterId())) {
                semesterError = appContext.getString(R.string.error_select_semester);
            }
        }

        uiState.setValue(current.toBuilder()
                .fullNameError(fullNameResult.errorMessageOrNull())
                .fatherNameError(fatherNameResult.errorMessageOrNull())
                .cnicError(cnicResult.errorMessageOrNull())
                .phoneError(phoneResult.errorMessageOrNull())
                .emailError(emailResult.errorMessageOrNull())
                .passwordError(passwordResult.errorMessageOrNull())
                .confirmPasswordError(confirmResult.errorMessageOrNull())
                .departmentError(departmentError)
                .sessionError(sessionError)
                .semesterError(semesterError)
                .build());

        boolean hasErrors = !fullNameResult.isValid() || !fatherNameResult.isValid() || !cnicResult.isValid()
                || !phoneResult.isValid() || !emailResult.isValid() || !passwordResult.isValid() || !confirmResult.isValid()
                || departmentError != null || sessionError != null || semesterError != null;
        if (hasErrors) return;

        RegisterUiState toSubmit = state();
        uiState.setValue(toSubmit.toBuilder().loading(true).submitError(null).build());

        authDataSource.register(
                toSubmit.getFullName().trim(),
                toSubmit.getFatherName().trim(),
                CnicValidator.normalize(toSubmit.getCnic()),
                PhoneValidator.normalize(toSubmit.getPhone()),
                toSubmit.getEmail().trim(),
                toSubmit.getPassword(),
                toSubmit.getRole(),
                toSubmit.getPhotoUri(),
                buildSelections(toSubmit)
        ).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                uiState.setValue(RegisterUiState.initial());
                registrationSubmitted.setValue(task.getResult());
            } else {
                String message = errorMessage(task.getException());
                uiState.setValue(state().toBuilder().loading(false).submitError(message).build());
            }
        });
    }

    private String validateDepartmentSelection(RegisterUiState s) {
        switch (s.getRole()) {
            case STUDENT:
                if (isBlank(s.getStudentDepartmentId())) {
                    return appContext.getString(R.string.error_select_department);
                }
                break;
            case TEACHER:
                if (s.getTeacherDepartmentIds().isEmpty()) {
                    return appContext.getString(R.string.error_select_department);
                }
                break;
            case HOD:
                if (isBlank(s.getHodDepartmentId())) {
                    return appContext.getString(R.string.error_select_department);
                }
                break;
            default:
                break;
        }
        return null;
    }

    private RegisterSelections buildSelections(RegisterUiState s) {
        switch (s.getRole()) {
            case STUDENT:
                return new RegisterSelections(s.getStudentDepartmentId(), s.getStudentSessionId(),
                        s.getStudentSemesterId(), null);
            case TEACHER:
                return new RegisterSelections(null, null, null, s.getTeacherDepartmentIds());
            case HOD:
                return new RegisterSelections(s.getHodDepartmentId(), null, null, null);
            default:
                return RegisterSelections.empty();
        }
    }

    // ---- Small helpers ----

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String errorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().isEmpty()) {
            return exception.getMessage();
        }
        return appContext.getString(R.string.generic_error_message);
    }
}
