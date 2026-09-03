package com.example.uos_lms.feature.profile.presentation;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.MediaApi;
import com.example.uos_lms.core.data.remote.api.MultipartFileUtils;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.data.remote.api.dto.MediaEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdatePhotoRequestDto;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/** Display-only Profile (photo management is the one edit action that stays here - tapping the
 * avatar to change it is the natural gesture on a profile screen). Editing text fields, password,
 * theme and logout all moved to SettingsFragment/SettingsViewModel. */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final android.content.Context appContext;
    private final ApiUniversityDataSource universityDataSource;
    private final MediaApi mediaApi;
    private final AuthApi authApi;

    private final MutableLiveData<ProfileUiState> uiState = new MutableLiveData<>(ProfileUiState.initial());

    @Inject
    public ProfileViewModel(
            @ApplicationContext android.content.Context appContext,
            ApiUniversityDataSource universityDataSource,
            MediaApi mediaApi,
            AuthApi authApi,
            SessionManager sessionManager) {
        this.appContext = appContext;
        this.universityDataSource = universityDataSource;
        this.mediaApi = mediaApi;
        this.authApi = authApi;

        CachedSession session = sessionManager.getCachedSession().getValue();
        if (session == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage("Session expired. Please log in again.").build());
            return;
        }
        load();
    }

    public LiveData<ProfileUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches the profile from the backend without blanking what's currently shown - no-ops
     * while a refresh is already in flight. Also the entry point for refreshing automatically
     * when returning from Settings (see Fragment.onResume), so a saved edit is reflected here
     * without needing to leave and reopen the app. */
    public void refresh() {
        ProfileUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    uiState.setValue(uiState.getValue().toBuilder().user(user).loading(false).refreshing(false).build());
                    if (user != null) resolveRoleContext(user);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    /** One-shot resolution of the names behind a user's id fields, plus a subjects count for
     * Teacher/Student. */
    private void resolveRoleContext(User user) {
        String departmentId = user.getDepartment();
        if (departmentId == null) return;

        // Teacher is multi-department (departmentIds); HOD/Student stay single (department).
        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            List<String> ids = user.getRole() == UserRole.TEACHER
                    ? user.getDepartmentIds()
                    : Collections.singletonList(departmentId);
            List<String> names = new ArrayList<>();
            for (String id : ids) {
                for (Department department : departments) {
                    if (department.getId().equals(id)) {
                        names.add(department.getName());
                        break;
                    }
                }
            }
            uiState.setValue(uiState.getValue().toBuilder().departmentName(String.join(", ", names)).build());
        });

        if (user.getSessionId() != null) {
            universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions -> {
                for (Session session : sessions) {
                    if (session.getId().equals(user.getSessionId())) {
                        uiState.setValue(uiState.getValue().toBuilder().sessionLabel(session.getLabel()).build());
                        break;
                    }
                }
            });
        }

        if (user.getSemester() != null) {
            universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> {
                for (Semester semester : semesters) {
                    if (semester.getId().equals(user.getSemester())) {
                        uiState.setValue(uiState.getValue().toBuilder().semesterLabel(semester.getDisplayName()).build());
                        break;
                    }
                }
            });
        }

        if (user.getRole() == UserRole.TEACHER) {
            universityDataSource.listSubjectsForTeacher(user.getUid()).addOnSuccessListener(subjects ->
                    uiState.setValue(uiState.getValue().toBuilder().subjectsCount(subjects.size()).build()));
        } else if (user.getRole() == UserRole.STUDENT && user.getSemester() != null) {
            universityDataSource.listSubjectsForSemester(user.getSemester()).addOnSuccessListener(subjects ->
                    uiState.setValue(uiState.getValue().toBuilder().subjectsCount(subjects.size()).build()));
        }
    }

    private static final String UPLOAD_LOG_TAG = "ProfilePhotoUpload";

    /** Uploads via the backend's Media API (Cloudinary credentials never touch the app - see
     * Phase 2 of the migration), writes the resulting url/publicId into the Mongo User doc
     * (self-service PATCH /auth/me/photo), and applies the returned updated User straight to
     * local state so the UI reflects it without an extra round trip. */
    public void updatePhoto(Uri uri) {
        User user = uiState.getValue().getUser();
        if (user == null) {
            Log.d(UPLOAD_LOG_TAG, "updatePhoto() aborted: user is null");
            return;
        }
        Log.d(UPLOAD_LOG_TAG, "updatePhoto() called for uid=" + user.getUid() + " uri=" + uri);

        uiState.setValue(uiState.getValue().toBuilder().uploadingPhoto(true).uploadProgress(null).errorMessage(null).build());

        Task<MediaEnvelopeDto> uploadTask;
        try {
            RequestBody category = RequestBody.create(MediaType.parse("text/plain"), "profile_photo");
            MultipartBody.Part filePart = MultipartFileUtils.filePart(appContext, uri, "file",
                    percent -> uiState.postValue(uiState.getValue().toBuilder().uploadProgress(percent).build()));
            uploadTask = RetrofitTasks.call(mediaApi.upload(category, null, null, filePart));
        } catch (IOException e) {
            Log.w(UPLOAD_LOG_TAG, "Could not prepare picked photo for upload: uri=" + uri, e);
            uiState.setValue(uiState.getValue().toBuilder()
                    .uploadingPhoto(false).uploadProgress(null).errorMessage(e.getMessage() != null ? e.getMessage() : "Could not read the selected file.").build());
            return;
        }

        uploadTask.onSuccessTask(envelope -> {
                    String url = envelope.getMedia().getSecureUrl();
                    String publicId = envelope.getMedia().getPublicId();
                    return RetrofitTasks.call(authApi.updateMyPhoto(new UpdatePhotoRequestDto(url, publicId)));
                })
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(updatedUser -> {
                    Log.d(UPLOAD_LOG_TAG, "profile photo update succeeded");
                    uiState.setValue(uiState.getValue().toBuilder().user(updatedUser).uploadingPhoto(false).uploadProgress(null).build());
                })
                .addOnFailureListener(e -> {
                    Log.e(UPLOAD_LOG_TAG, "updatePhoto() chain failed", e);
                    uiState.setValue(uiState.getValue().toBuilder()
                            .uploadingPhoto(false).uploadProgress(null).errorMessage(e.getMessage()).build());
                });
    }

    public void removePhoto() {
        User user = uiState.getValue().getUser();
        if (user == null) return;

        uiState.setValue(uiState.getValue().toBuilder().uploadingPhoto(true).errorMessage(null).build());

        // Best-effort delete from Cloudinary (via the backend) - even if it fails, still
        // clear the profile fields so the app doesn't keep pointing at a removed photo.
        RetrofitTasks.call(mediaApi.deleteMyProfilePhoto())
                .continueWithTask(ignored -> RetrofitTasks.call(authApi.updateMyPhoto(new UpdatePhotoRequestDto(null, null))))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(updatedUser -> uiState.setValue(uiState.getValue().toBuilder().user(updatedUser).uploadingPhoto(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().uploadingPhoto(false).errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
