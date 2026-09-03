package com.example.uos_lms.feature.admin.presentation.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.auth.data.AuthDataSource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminUserListViewModel extends ViewModel {

    private final ApiUserDataSource userDataSource;
    private final AuthDataSource authDataSource;

    private final MutableLiveData<AdminDashboardUiState> uiState =
            new MutableLiveData<>(AdminDashboardUiState.initial());

    @Inject
    public AdminUserListViewModel(ApiUserDataSource userDataSource, AuthDataSource authDataSource) {
        this.userDataSource = userDataSource;
        this.authDataSource = authDataSource;
        load();
    }

    private void load() {
        userDataSource.allUsers()
                .addOnSuccessListener(users -> uiState.setValue(uiState.getValue().toBuilder().allUsers(users).loading(false).refreshing(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<AdminDashboardUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches every user from the backend without blanking the currently-shown list,
     * preserving the user's current search/filter - no-ops while a refresh is already in
     * flight. */
    public void refresh() {
        AdminDashboardUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    public void onSearchQueryChange(String query) {
        uiState.setValue(uiState.getValue().toBuilder().searchQuery(query).build());
    }

    public void onFilterChange(UserFilter filter) {
        uiState.setValue(uiState.getValue().toBuilder().selectedFilter(filter).build());
    }

    public void approve(String uid) {
        updateStatus(uid, UserStatus.APPROVED, null, "User approved.");
    }

    public void reject(String uid) {
        updateStatus(uid, UserStatus.REJECTED, "Rejected by Admin.", "User rejected.");
    }

    public void suspendUser(String uid) {
        updateStatus(uid, UserStatus.SUSPENDED, "Suspended by Admin.", "User suspended.");
    }

    public void activate(String uid) {
        updateStatus(uid, UserStatus.APPROVED, null, "User activated.");
    }

    private void updateStatus(String uid, UserStatus status, String reason, String successMessage) {
        userDataSource.updateStatus(uid, status, reason)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage(successMessage).build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void delete(String uid) {
        userDataSource.deleteUser(uid)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("User deleted.").build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void resetPassword(String email) {
        authDataSource.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().actionMessage("A password reset code has been emailed to the user.").build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
