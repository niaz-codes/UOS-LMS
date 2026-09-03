package com.example.uos_lms.feature.auth.presentation.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class LoginUiState {
    @Builder.Default
    private final String email = "";
    @Builder.Default
    private final String password = "";
    private final boolean loading;
    private final String errorMessage;

    private final boolean showForgotPasswordDialog;
    @Builder.Default
    private final String forgotPasswordEmail = "";
    private final boolean sendingPasswordReset;
    private final String forgotPasswordError;

    // Step 2 of the dialog: the emailed code + new password, shown once sendPasswordReset()
    // succeeds (codeSent). resetPasswordSuccess is the final "you're done" state.
    private final boolean codeSent;
    @Builder.Default
    private final String resetCode = "";
    @Builder.Default
    private final String newPassword = "";
    @Builder.Default
    private final String confirmNewPassword = "";
    private final boolean resettingPassword;
    private final boolean resetPasswordSuccess;

    public static LoginUiState initial() {
        return LoginUiState.builder().build();
    }
}
