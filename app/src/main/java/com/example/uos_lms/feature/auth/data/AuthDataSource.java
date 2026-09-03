package com.example.uos_lms.feature.auth.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.MultipartFileUtils;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.data.remote.api.dto.ChangePasswordRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.ForgotPasswordRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.LoginRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RegisterRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.ResetPasswordRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UserEnvelopeDto;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Pure backend-REST auth. Identity, approval, and credentials all live in the Node/Express/
 * MongoDB backend (see backend/src/controllers/authController.js) - there is no Firebase
 * Auth involved anywhere in this class anymore. login()/register() call the backend via
 * {@link AuthApi} and return Task<T> via {@link RetrofitTasks}, matching the app-wide async idiom.
 */
@Singleton
public class AuthDataSource {

    private static final String TAG = "AuthDataSource";

    private final Context appContext;
    private final AuthApi authApi;
    private final SessionManager sessionManager;

    @Inject
    public AuthDataSource(@ApplicationContext Context appContext, AuthApi authApi, SessionManager sessionManager) {
        this.appContext = appContext;
        this.authApi = authApi;
        this.sessionManager = sessionManager;
    }

    public Task<User> login(String email, String password) {
        String trimmedEmail = email.trim();
        return RetrofitTasks.call(authApi.login(new LoginRequestDto(trimmedEmail, password)))
                .onSuccessTask(authResponse -> {
                    sessionManager.saveAuthToken(authResponse.getToken());
                    return Tasks.forResult(authResponse.getUser().toDomain());
                });
    }

    public Task<User> register(String fullName, String fatherName, String cnic, String phone,
                                String email, String password, UserRole role, @Nullable Uri photoUri,
                                RegisterSelections selections) {
        if (!UserRole.REGISTERABLE_ROLES.contains(role)) {
            return Tasks.forException(new IllegalArgumentException("Invalid role for registration."));
        }
        if (selections == null) {
            selections = RegisterSelections.empty();
        }
        final RegisterSelections finalSelections = selections;

        return doRegister(fullName, fatherName, cnic, phone, email, password, role, photoUri, finalSelections)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        // The backend generates the student's Registration Number server-side and
                        // returns it here so the success screen can show it to the applicant.
                        return Tasks.forResult(task.getResult().getUser().toDomain());
                    }
                    Exception exception = task.getException() != null
                            ? task.getException() : new Exception("Registration failed. Please try again.");
                    return Tasks.forException(exception);
                });
    }

    private Task<UserEnvelopeDto> doRegister(
            String fullName, String fatherName, String cnic, String phone,
            String email, String password, UserRole role, @Nullable Uri photoUri,
            RegisterSelections selections) {
        if (photoUri == null) {
            RegisterRequestDto request = RegisterRequestDto.builder()
                    .fullName(fullName)
                    .fatherName(fatherName)
                    .cnic(cnic)
                    .phone(phone)
                    .email(email.trim())
                    .password(password)
                    .role(role.name())
                    .departmentId(selections.getDepartmentId())
                    .sessionId(selections.getSessionId())
                    .currentSemesterId(selections.getSemesterId())
                    .departmentIds(selections.getDepartmentIds().isEmpty() ? null : selections.getDepartmentIds())
                    .build();
            return RetrofitTasks.call(authApi.register(request));
        }

        MultipartBody.Part photoPart;
        try {
            photoPart = MultipartFileUtils.filePart(appContext, photoUri, "photo", null);
        } catch (IOException e) {
            Log.w(TAG, "Could not prepare picked registration photo for upload: uri=" + photoUri, e);
            return Tasks.forException(e);
        }
        return RetrofitTasks.call(authApi.registerWithPhoto(
                text(fullName), text(fatherName), text(cnic), text(phone),
                text(email.trim()), text(password), text(role.name()),
                text(selections.getDepartmentId()), text(selections.getSessionId()), text(selections.getSemesterId()),
                toTextParts(selections.getDepartmentIds()),
                photoPart));
    }

    private static RequestBody text(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return RequestBody.create(MediaType.parse("text/plain"), value.trim());
    }

    private static java.util.List<RequestBody> toTextParts(java.util.List<String> values) {
        java.util.List<RequestBody> parts = new java.util.ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    parts.add(RequestBody.create(MediaType.parse("text/plain"), value.trim()));
                }
            }
        }
        return parts;
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return Tasks.forException(new IllegalStateException("Email is required."));
        }
        return RetrofitTasks.call(authApi.forgotPassword(new ForgotPasswordRequestDto(trimmed)));
    }

    public Task<Void> resetPassword(String email, String code, String newPassword) {
        return RetrofitTasks.call(authApi.resetPassword(new ResetPasswordRequestDto(email.trim(), code.trim(), newPassword)));
    }

    public Task<Void> changePassword(String currentPassword, String newPassword) {
        return RetrofitTasks.call(authApi.changePassword(new ChangePasswordRequestDto(currentPassword, newPassword)));
    }

    public Task<User> getCurrentUserProfile() {
        if (sessionManager.getAuthToken() == null) {
            return Tasks.<User>forResult(null);
        }
        return RetrofitTasks.call(authApi.me()).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Session token rejected, clearing cached session", task.getException());
                sessionManager.clear();
                return null;
            }
            return task.getResult().getUser().toDomain();
        });
    }
}
