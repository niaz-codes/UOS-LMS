package com.example.uos_lms.feature.auth.presentation.login;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.session.ServerConfig;
import com.example.uos_lms.core.ui.AnimUtils;
import com.example.uos_lms.core.ui.InsetUtils;
import com.example.uos_lms.feature.auth.presentation.AuthNavigator;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Inject
    ServerConfig serverConfig;

    private LoginViewModel viewModel;

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private MaterialButton buttonSignIn;
    private CircularProgressIndicator progressSignIn;

    private Dialog forgotPasswordDialog;
    private TextInputLayout dialogEmailLayout;
    private TextInputEditText dialogEmailEdit;
    private TextInputLayout dialogCodeLayout;
    private TextInputEditText dialogCodeEdit;
    private TextInputLayout dialogNewPasswordLayout;
    private TextInputEditText dialogNewPasswordEdit;
    private TextInputLayout dialogConfirmNewPasswordLayout;
    private TextInputEditText dialogConfirmNewPasswordEdit;
    private TextView dialogMessage;
    private CircularProgressIndicator dialogProgress;

    private boolean suppressEmailWatcher;
    private boolean suppressPasswordWatcher;
    private boolean suppressDialogEmailWatcher;
    private boolean suppressDialogCodeWatcher;
    private boolean suppressDialogNewPasswordWatcher;
    private boolean suppressDialogConfirmNewPasswordWatcher;

    @Nullable
    private String lastErrorMessage;

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        layoutEmail = view.findViewById(R.id.layoutEmail);
        layoutPassword = view.findViewById(R.id.layoutPassword);
        editEmail = view.findViewById(R.id.editEmail);
        editPassword = view.findViewById(R.id.editPassword);
        buttonSignIn = view.findViewById(R.id.buttonSignIn);
        progressSignIn = view.findViewById(R.id.progressSignIn);
        MaterialButton buttonRegister = view.findViewById(R.id.buttonRegister);
        TextView textForgotPassword = view.findViewById(R.id.textForgotPassword);

        editEmail.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (!suppressEmailWatcher) viewModel.onEmailChange(text);
        }));
        editPassword.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (!suppressPasswordWatcher) viewModel.onPasswordChange(text);
        }));

        buttonSignIn.setOnClickListener(v -> viewModel.login());
        buttonRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_loginFragment_to_registerFragment));
        textForgotPassword.setOnClickListener(v -> viewModel.onForgotPasswordClick());

        AnimUtils.applyPressScale(buttonSignIn);

        View imageLogo = view.findViewById(R.id.imageLogo);
        View cardLoginForm = view.findViewById(R.id.cardLoginForm);
        InsetUtils.applyStatusBarTopMargin(imageLogo, 24);
        AnimUtils.revealLogo(imageLogo);
        AnimUtils.fadeSlideIn(cardLoginForm, 150L);
        imageLogo.setOnLongClickListener(v -> {
            showServerConfigDialog();
            return true;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getNavigation().observe(getViewLifecycleOwner(), destination -> {
            if (destination == null) return;
            AuthNavigator.navigate(NavHostFragment.findNavController(this), destination, R.id.loginFragment);
        });
    }

    private void render(LoginUiState state) {
        suppressEmailWatcher = true;
        if (!state.getEmail().equals(safe(editEmail))) editEmail.setText(state.getEmail());
        suppressEmailWatcher = false;

        suppressPasswordWatcher = true;
        if (!state.getPassword().equals(safe(editPassword))) editPassword.setText(state.getPassword());
        suppressPasswordWatcher = false;

        layoutPassword.setError(state.getErrorMessage());
        layoutEmail.setErrorEnabled(false);

        if (state.getErrorMessage() != null && !state.getErrorMessage().equals(lastErrorMessage)) {
            AnimUtils.shake(layoutPassword);
        }
        lastErrorMessage = state.getErrorMessage();

        boolean loading = state.isLoading();
        buttonSignIn.setEnabled(!loading);
        buttonSignIn.setText(loading ? "" : getString(R.string.sign_in));
        progressSignIn.animate().cancel();
        if (loading) {
            progressSignIn.setAlpha(0f);
            progressSignIn.setVisibility(View.VISIBLE);
            progressSignIn.animate().alpha(1f).setDuration(150L).start();
        } else if (progressSignIn.getVisibility() == View.VISIBLE) {
            progressSignIn.animate().alpha(0f).setDuration(150L)
                    .withEndAction(() -> progressSignIn.setVisibility(View.GONE)).start();
        }

        renderForgotPasswordDialog(state);
    }

    private void renderForgotPasswordDialog(LoginUiState state) {
        if (!state.isShowForgotPasswordDialog()) {
            if (forgotPasswordDialog != null) {
                forgotPasswordDialog.dismiss();
                forgotPasswordDialog = null;
            }
            return;
        }

        if (forgotPasswordDialog == null) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_forgot_password, null);
            dialogEmailLayout = dialogView.findViewById(R.id.layoutEmail);
            dialogEmailEdit = dialogView.findViewById(R.id.editEmail);
            dialogCodeLayout = dialogView.findViewById(R.id.layoutCode);
            dialogCodeEdit = dialogView.findViewById(R.id.editCode);
            dialogNewPasswordLayout = dialogView.findViewById(R.id.layoutNewPassword);
            dialogNewPasswordEdit = dialogView.findViewById(R.id.editNewPassword);
            dialogConfirmNewPasswordLayout = dialogView.findViewById(R.id.layoutConfirmNewPassword);
            dialogConfirmNewPasswordEdit = dialogView.findViewById(R.id.editConfirmNewPassword);
            dialogMessage = dialogView.findViewById(R.id.textMessage);
            dialogProgress = dialogView.findViewById(R.id.progressSending);

            dialogEmailEdit.addTextChangedListener(new SimpleTextWatcher(text -> {
                if (!suppressDialogEmailWatcher) viewModel.onForgotPasswordEmailChange(text);
            }));
            dialogCodeEdit.addTextChangedListener(new SimpleTextWatcher(text -> {
                if (!suppressDialogCodeWatcher) viewModel.onResetCodeChange(text);
            }));
            dialogNewPasswordEdit.addTextChangedListener(new SimpleTextWatcher(text -> {
                if (!suppressDialogNewPasswordWatcher) viewModel.onNewPasswordChange(text);
            }));
            dialogConfirmNewPasswordEdit.addTextChangedListener(new SimpleTextWatcher(text -> {
                if (!suppressDialogConfirmNewPasswordWatcher) viewModel.onConfirmNewPasswordChange(text);
            }));

            forgotPasswordDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.forgot_password_title)
                    .setView(dialogView)
                    .setPositiveButton(R.string.send_reset_link, null)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> viewModel.onForgotPasswordDismiss())
                    .setOnDismissListener(dialog -> viewModel.onForgotPasswordDismiss())
                    .setCancelable(true)
                    .show();

            ((androidx.appcompat.app.AlertDialog) forgotPasswordDialog)
                    .getButton(DialogInterface.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        LoginUiState current = viewModel.getUiState().getValue();
                        if (current.isResetPasswordSuccess()) {
                            viewModel.onForgotPasswordDismiss();
                        } else if (current.isCodeSent()) {
                            viewModel.submitPasswordReset();
                        } else {
                            viewModel.sendPasswordReset();
                        }
                    });
        }

        suppressDialogEmailWatcher = true;
        if (!state.getForgotPasswordEmail().equals(safe(dialogEmailEdit))) {
            dialogEmailEdit.setText(state.getForgotPasswordEmail());
        }
        suppressDialogEmailWatcher = false;

        suppressDialogCodeWatcher = true;
        if (!state.getResetCode().equals(safe(dialogCodeEdit))) dialogCodeEdit.setText(state.getResetCode());
        suppressDialogCodeWatcher = false;

        suppressDialogNewPasswordWatcher = true;
        if (!state.getNewPassword().equals(safe(dialogNewPasswordEdit))) dialogNewPasswordEdit.setText(state.getNewPassword());
        suppressDialogNewPasswordWatcher = false;

        suppressDialogConfirmNewPasswordWatcher = true;
        if (!state.getConfirmNewPassword().equals(safe(dialogConfirmNewPasswordEdit))) {
            dialogConfirmNewPasswordEdit.setText(state.getConfirmNewPassword());
        }
        suppressDialogConfirmNewPasswordWatcher = false;

        boolean busy = state.isSendingPasswordReset() || state.isResettingPassword();
        dialogProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        dialogEmailEdit.setEnabled(!busy);
        dialogCodeEdit.setEnabled(!busy);
        dialogNewPasswordEdit.setEnabled(!busy);
        dialogConfirmNewPasswordEdit.setEnabled(!busy);

        androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog) forgotPasswordDialog;
        MaterialButton positive = (MaterialButton) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        MaterialButton negative = (MaterialButton) dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        positive.setEnabled(!busy);

        if (state.isResetPasswordSuccess()) {
            dialogMessage.setText(R.string.password_reset_success_message);
            dialogEmailLayout.setVisibility(View.GONE);
            dialogCodeLayout.setVisibility(View.GONE);
            dialogNewPasswordLayout.setVisibility(View.GONE);
            dialogConfirmNewPasswordLayout.setVisibility(View.GONE);
            positive.setText(R.string.done);
            negative.setVisibility(View.GONE);
            dialogEmailLayout.setError(null);
            dialogCodeLayout.setError(null);
            dialogNewPasswordLayout.setError(null);
        } else if (state.isCodeSent()) {
            dialogMessage.setText(R.string.forgot_password_code_message);
            dialogEmailLayout.setVisibility(View.GONE);
            dialogCodeLayout.setVisibility(View.VISIBLE);
            dialogNewPasswordLayout.setVisibility(View.VISIBLE);
            dialogConfirmNewPasswordLayout.setVisibility(View.VISIBLE);
            positive.setText(R.string.reset_password_button);
            negative.setVisibility(View.VISIBLE);
            dialogEmailLayout.setError(null);
            dialogCodeLayout.setError(state.getForgotPasswordError());
        } else {
            dialogMessage.setText(R.string.forgot_password_message);
            dialogEmailLayout.setVisibility(View.VISIBLE);
            dialogCodeLayout.setVisibility(View.GONE);
            dialogNewPasswordLayout.setVisibility(View.GONE);
            dialogConfirmNewPasswordLayout.setVisibility(View.GONE);
            positive.setText(R.string.send_reset_link);
            negative.setVisibility(View.VISIBLE);
            dialogEmailLayout.setError(state.getForgotPasswordError());
        }
    }

    /** Long-press the logo to point the app at a different backend host:port at runtime -
     * see {@link ServerConfig} for why this exists instead of a rebuild-only BuildConfig value. */
    private void showServerConfigDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_server_config, null);
        TextInputEditText editHostPort = dialogView.findViewById(R.id.editServerHostPort);
        String current = serverConfig.getOverrideHostPort();
        if (current != null) editHostPort.setText(current);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.server_config_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String value = editHostPort.getText() == null ? "" : editHostPort.getText().toString().trim();
                    serverConfig.setOverrideHostPort(value);
                    Toast.makeText(requireContext(), R.string.server_config_saved, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.server_config_reset, (dialog, which) -> {
                    serverConfig.setOverrideHostPort(null);
                    Toast.makeText(requireContext(), R.string.server_config_reset, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static String safe(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    @Override
    public void onDestroyView() {
        if (forgotPasswordDialog != null) {
            forgotPasswordDialog.dismiss();
            forgotPasswordDialog = null;
        }
        super.onDestroyView();
    }

    interface TextChangedCallback {
        void onChanged(String text);
    }

    static class SimpleTextWatcher implements TextWatcher {
        private final TextChangedCallback callback;

        SimpleTextWatcher(TextChangedCallback callback) {
            this.callback = callback;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            callback.onChanged(s.toString());
        }
    }
}
