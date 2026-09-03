package com.example.uos_lms.feature.auth.presentation.login;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.common.SingleLiveEvent;
import com.example.uos_lms.core.data.remote.api.ApiException;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.core.session.SessionManager;
import com.example.uos_lms.core.validation.EmailValidator;
import com.example.uos_lms.core.validation.PasswordValidator;
import com.example.uos_lms.core.validation.ValidationResult;
import com.example.uos_lms.feature.auth.data.AuthDataSource;
import com.example.uos_lms.feature.auth.presentation.AuthDestination;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthDataSource authDataSource;
    private final SessionManager sessionManager;

    private final MutableLiveData<LoginUiState> uiState = new MutableLiveData<>(LoginUiState.initial());
    private final SingleLiveEvent<AuthDestination> navigation = new SingleLiveEvent<>();

    @Inject
    public LoginViewModel(AuthDataSource authDataSource, SessionManager sessionManager) {
        this.authDataSource = authDataSource;
        this.sessionManager = sessionManager;
    }

    public LiveData<LoginUiState> getUiState() {
        return uiState;
    }

    public LiveData<AuthDestination> getNavigation() {
        return navigation;
    }

    private LoginUiState state() {
        return uiState.getValue();
    }

    public void onEmailChange(String value) {
        uiState.setValue(state().toBuilder().email(value).errorMessage(null).build());
    }

    public void onPasswordChange(String value) {
        uiState.setValue(state().toBuilder().password(value).errorMessage(null).build());
    }

    public void login() {
        LoginUiState current = state();
        String emailError = EmailValidator.validate(current.getEmail()).errorMessageOrNull();
        if (emailError != null) {
            uiState.setValue(current.toBuilder().errorMessage(emailError).build());
            return;
        }
        if (current.getPassword().isEmpty()) {
            uiState.setValue(current.toBuilder().errorMessage("Password is required").build());
            return;
        }

        uiState.setValue(current.toBuilder().loading(true).errorMessage(null).build());
        authDataSource.login(current.getEmail().trim(), current.getPassword()).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                // The backend rejects non-approved accounts with a 403 that carries which
                // status they're in, so login() never returns a non-APPROVED user anymore -
                // route to the matching status screen instead of just showing an error.
                UserStatus accountStatus = accountStatusOf(task.getException());
                if (accountStatus != null) {
                    uiState.setValue(state().toBuilder().loading(false).build());
                    switch (accountStatus) {
                        case PENDING:
                            navigation.setValue(AuthDestination.pending());
                            break;
                        case REJECTED:
                            navigation.setValue(AuthDestination.rejected());
                            break;
                        case SUSPENDED:
                            navigation.setValue(AuthDestination.suspended());
                            break;
                        default:
                            break;
                    }
                    return;
                }

                uiState.setValue(state().toBuilder().loading(false).errorMessage(messageOf(task.getException())).build());
                return;
            }

            User user = task.getResult();
            sessionManager.cacheAsTask(user.getUid(), user.getRole(), user.getFullName())
                    .addOnCompleteListener(ignored -> {
                        uiState.setValue(state().toBuilder().loading(false).build());
                        navigation.setValue(AuthDestination.dashboard(user.getRole()));
                    });
        });
    }

    @Nullable
    private static UserStatus accountStatusOf(@Nullable Exception exception) {
        if (!(exception instanceof ApiException)) return null;
        return UserStatus.fromStringOrNull(((ApiException) exception).getAccountStatus());
    }

    public void onForgotPasswordClick() {
        LoginUiState current = state();
        uiState.setValue(current.toBuilder()
                .showForgotPasswordDialog(true)
                .forgotPasswordEmail(current.getEmail())
                .forgotPasswordError(null)
                .codeSent(false)
                .resetCode("")
                .newPassword("")
                .confirmNewPassword("")
                .resetPasswordSuccess(false)
                .build());
    }

    public void onForgotPasswordDismiss() {
        uiState.setValue(state().toBuilder()
                .showForgotPasswordDialog(false)
                .codeSent(false)
                .resetPasswordSuccess(false)
                .build());
    }

    public void onForgotPasswordEmailChange(String value) {
        uiState.setValue(state().toBuilder().forgotPasswordEmail(value).forgotPasswordError(null).build());
    }

    public void onResetCodeChange(String value) {
        uiState.setValue(state().toBuilder().resetCode(value).forgotPasswordError(null).build());
    }

    public void onNewPasswordChange(String value) {
        uiState.setValue(state().toBuilder().newPassword(value).forgotPasswordError(null).build());
    }

    public void onConfirmNewPasswordChange(String value) {
        uiState.setValue(state().toBuilder().confirmNewPassword(value).forgotPasswordError(null).build());
    }

    /** Step 1: email a one-time code. */
    public void sendPasswordReset() {
        LoginUiState current = state();
        String emailError = EmailValidator.validate(current.getForgotPasswordEmail()).errorMessageOrNull();
        if (emailError != null) {
            uiState.setValue(current.toBuilder().forgotPasswordError(emailError).build());
            return;
        }

        uiState.setValue(current.toBuilder().sendingPasswordReset(true).forgotPasswordError(null).build());
        authDataSource.sendPasswordResetEmail(current.getForgotPasswordEmail()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uiState.setValue(state().toBuilder().sendingPasswordReset(false).codeSent(true).build());
            } else {
                uiState.setValue(state().toBuilder().sendingPasswordReset(false).forgotPasswordError(messageOf(task.getException())).build());
            }
        });
    }

    /** Step 2: submit the emailed code + a new password. */
    public void submitPasswordReset() {
        LoginUiState current = state();
        if (current.getResetCode().trim().isEmpty()) {
            uiState.setValue(current.toBuilder().forgotPasswordError("Enter the code from your email.").build());
            return;
        }
        ValidationResult passwordResult = PasswordValidator.validate(current.getNewPassword());
        if (!passwordResult.isValid()) {
            uiState.setValue(current.toBuilder().forgotPasswordError(passwordResult.errorMessageOrNull()).build());
            return;
        }
        ValidationResult confirmResult = PasswordValidator.validateConfirmation(current.getNewPassword(), current.getConfirmNewPassword());
        if (!confirmResult.isValid()) {
            uiState.setValue(current.toBuilder().forgotPasswordError(confirmResult.errorMessageOrNull()).build());
            return;
        }

        uiState.setValue(current.toBuilder().resettingPassword(true).forgotPasswordError(null).build());
        authDataSource.resetPassword(current.getForgotPasswordEmail(), current.getResetCode(), current.getNewPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uiState.setValue(state().toBuilder().resettingPassword(false).resetPasswordSuccess(true).build());
                    } else {
                        uiState.setValue(state().toBuilder().resettingPassword(false).forgotPasswordError(messageOf(task.getException())).build());
                    }
                });
    }

    private static String messageOf(@Nullable Exception exception) {
        return exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "Something went wrong. Please try again.";
    }
}
