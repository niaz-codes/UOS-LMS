package com.example.uos_lms.feature.profile.presentation.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.example.uos_lms.core.session.ThemeMode;
import com.example.uos_lms.core.session.ThemePreferenceManager;
import com.example.uos_lms.feature.auth.data.AuthDataSource;
import com.google.android.gms.tasks.Tasks;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Everything that used to live inline in ProfileFragment except photo management (which stays
 * on Profile, tap-the-avatar being the natural gesture for it) - Edit Profile, Change Password,
 * theme, and Logout, all moved here verbatim with the Settings screen as their new home. */
@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final ApiUserDataSource userDataSource;
    private final AuthDataSource authDataSource;
    private final SessionManager sessionManager;
    private final ThemePreferenceManager themePreferenceManager;

    private final MutableLiveData<SettingsUiState> uiState = new MutableLiveData<>(SettingsUiState.initial());

    @Inject
    public SettingsViewModel(
            ApiUserDataSource userDataSource,
            AuthApi authApi,
            AuthDataSource authDataSource,
            SessionManager sessionManager,
            ThemePreferenceManager themePreferenceManager) {
        this.userDataSource = userDataSource;
        this.authDataSource = authDataSource;
        this.sessionManager = sessionManager;
        this.themePreferenceManager = themePreferenceManager;

        CachedSession session = sessionManager.getCachedSession().getValue();
        if (session == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage("Session expired. Please log in again.").build());
            return;
        }
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> uiState.setValue(uiState.getValue().toBuilder().user(user).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<SettingsUiState> getUiState() {
        return uiState;
    }

    public void updateProfile(String fullName, String fatherName, String phone, String cnic) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        if (fullName.isBlank()) {
            uiState.setValue(uiState.getValue().toBuilder().errorMessage("Full name is required.").build());
            return;
        }

        uiState.setValue(uiState.getValue().toBuilder().savingProfile(true).errorMessage(null).build());
        userDataSource.updateProfile(user.getUid(), fullName, fatherName, phone, cnic)
                .addOnSuccessListener(updated -> uiState.setValue(uiState.getValue().toBuilder().user(updated).savingProfile(false).actionMessage("Profile updated.").build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().savingProfile(false).errorMessage(e.getMessage()).build()));
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (newPassword.length() < 6) {
            uiState.setValue(uiState.getValue().toBuilder().errorMessage("New password must be at least 6 characters.").build());
            return;
        }

        uiState.setValue(uiState.getValue().toBuilder().changingPassword(true).errorMessage(null).build());
        authDataSource.changePassword(oldPassword, newPassword)
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().changingPassword(false).actionMessage("Password changed.").build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().changingPassword(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<ThemeMode> getThemeMode() {
        return themePreferenceManager.getThemeMode();
    }

    public void setThemeMode(ThemeMode mode) {
        themePreferenceManager.setThemeMode(mode);
    }

    public void clearMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }

    public void logout() {
        sessionManager.clear();
    }
}
