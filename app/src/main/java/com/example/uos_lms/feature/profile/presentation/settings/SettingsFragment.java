package com.example.uos_lms.feature.profile.presentation.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.BuildConfig;
import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.session.ThemeMode;
import com.example.uos_lms.core.ui.AnimUtils;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.feature.auth.presentation.AuthNavigator;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

/** Settings screen reached from Profile's gear icon - Edit Profile, Change Password, theme,
 * Notifications and Logout, all moved here from what used to be one crowded ProfileFragment.
 * Edit Profile / Change Password dialogs are the same dialogs, same layouts, same validation as
 * before, just triggered from this screen instead. */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.settings_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        bindAction(view, R.id.actionEditProfile, R.drawable.ic_edit, R.string.action_edit_profile_title, R.string.action_edit_profile_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> showEditProfileDialog());
        bindAction(view, R.id.actionAccountInfo, R.drawable.ic_badge, R.string.action_account_info_title, R.string.action_account_info_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> showAccountInfoDialog());
        bindAction(view, R.id.actionNotifications, R.drawable.ic_campaign, R.string.action_notifications_title, R.string.action_notifications_subtitle,
                R.color.admin_container, R.color.on_admin_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.notificationsFragment));
        bindAction(view, R.id.actionNotificationSettings, R.drawable.ic_notifications, R.string.action_notification_settings_title, R.string.action_notification_settings_subtitle,
                R.color.admin_container, R.color.on_admin_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.notificationSettingsFragment));
        bindAction(view, R.id.actionChangePassword, R.drawable.ic_lock_reset, R.string.action_change_password_title, R.string.action_change_password_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> showChangePasswordDialog());
        bindAction(view, R.id.actionAbout, R.drawable.ic_verified_user, R.string.action_about_title, R.string.action_about_subtitle,
                R.color.student_container, R.color.on_student_container,
                v -> showAboutDialog());
        bindAction(view, R.id.actionHelp, R.drawable.ic_help, R.string.action_help_title, R.string.action_help_subtitle,
                R.color.student_container, R.color.on_student_container,
                v -> showHelpDialog());

        ImageView imageThemeIcon = view.findViewById(R.id.imageThemeIcon);
        TextView textCurrentTheme = view.findViewById(R.id.textCurrentTheme);
        MaterialButtonToggleGroup toggleTheme = view.findViewById(R.id.toggleTheme);
        toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            viewModel.setThemeMode(themeModeForButtonId(checkedId));
        });
        viewModel.getThemeMode().observe(getViewLifecycleOwner(), mode -> {
            ThemeMode current = mode != null ? mode : ThemeMode.SYSTEM;
            toggleTheme.check(themeButtonId(current));
            imageThemeIcon.setImageResource(isEffectivelyDark() ? R.drawable.ic_dark_mode : R.drawable.ic_light_mode);
            textCurrentTheme.setText(getString(R.string.current_theme_format, themeDisplayName(current)));
        });

        ((TextView) view.findViewById(R.id.textAppVersion)).setText(getString(R.string.app_version_format, BuildConfig.VERSION_NAME));

        view.findViewById(R.id.buttonLogout).setOnClickListener(v ->
                ConfirmDialogHelper.show(requireContext(), getString(R.string.logout_confirm_title),
                        getString(R.string.logout_confirm_message), getString(R.string.logout_confirm_button), () -> {
                            viewModel.logout();
                            AuthNavigator.navigateToLoginClearingStack(NavHostFragment.findNavController(this));
                        }));

        AnimUtils.fadeSlideIn(view.findViewById(R.id.cardsContainer));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.clearMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });
    }

    private boolean isEffectivelyDark() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private String themeDisplayName(ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return getString(R.string.theme_light);
            case DARK:
                return getString(R.string.theme_dark);
            default:
                return getString(R.string.theme_system);
        }
    }

    private void showAccountInfoDialog() {
        User user = viewModel.getUiState().getValue() != null ? viewModel.getUiState().getValue().getUser() : null;
        if (user == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_account_information, null);
        setDetailRow(dialogView, R.id.rowFullName, R.string.full_name, user.getFullName());
        setDetailRow(dialogView, R.id.rowEmail, R.string.email, user.getEmail());
        setDetailRow(dialogView, R.id.rowRole, R.string.label_role, user.getRole().name());
        setDetailRow(dialogView, R.id.rowStatus, R.string.stat_account_status, user.getStatus() != null ? user.getStatus().name() : "-");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_account_info_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void setDetailRow(View root, int rowId, int labelRes, String value) {
        View row = root.findViewById(rowId);
        ((TextView) row.findViewById(R.id.textLabel)).setText(labelRes);
        ((TextView) row.findViewById(R.id.textValue)).setText(value);
    }

    private void showHelpDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_help_title)
                .setMessage(R.string.help_support_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void bindAction(View root, int includeId, int iconRes, int titleRes, int subtitleRes,
                             int containerColorRes, int onContainerColorRes, View.OnClickListener listener) {
        View card = root.findViewById(includeId);
        card.findViewById(R.id.iconBackground).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), containerColorRes)));
        ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), onContainerColorRes));
        ((TextView) card.findViewById(R.id.textTitle)).setText(titleRes);
        ((TextView) card.findViewById(R.id.textSubtitle)).setText(subtitleRes);
        card.setOnClickListener(listener);
    }

    private void showEditProfileDialog() {
        User user = viewModel.getUiState().getValue() != null ? viewModel.getUiState().getValue().getUser() : null;
        if (user == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText editFullName = dialogView.findViewById(R.id.editFullName);
        TextInputEditText editFatherName = dialogView.findViewById(R.id.editFatherName);
        TextInputEditText editPhone = dialogView.findViewById(R.id.editPhone);
        TextInputEditText editCnic = dialogView.findViewById(R.id.editCnic);
        TextView textError = dialogView.findViewById(R.id.textError);

        editFullName.setText(user.getFullName());
        editFatherName.setText(user.getFatherName());
        editPhone.setText(user.getPhone());
        editCnic.setText(user.getCnic());

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_profile_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String fullName = editFullName.getText() == null ? "" : editFullName.getText().toString().trim();
            if (fullName.isEmpty()) {
                textError.setText(R.string.full_name_required);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            String fatherName = editFatherName.getText() == null ? "" : editFatherName.getText().toString().trim();
            String phone = editPhone.getText() == null ? "" : editPhone.getText().toString().trim();
            String cnic = editCnic.getText() == null ? "" : editCnic.getText().toString().trim();
            viewModel.updateProfile(fullName, fatherName, phone, cnic);
            dialog.dismiss();
        });
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        TextInputEditText editOldPassword = dialogView.findViewById(R.id.editOldPassword);
        TextInputEditText editNewPassword = dialogView.findViewById(R.id.editNewPassword);
        TextInputEditText editConfirmPassword = dialogView.findViewById(R.id.editConfirmPassword);
        TextView textError = dialogView.findViewById(R.id.textError);

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.change_password_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String oldPassword = editOldPassword.getText() == null ? "" : editOldPassword.getText().toString();
            String newPassword = editNewPassword.getText() == null ? "" : editNewPassword.getText().toString();
            String confirmPassword = editConfirmPassword.getText() == null ? "" : editConfirmPassword.getText().toString();

            if (oldPassword.isBlank() || newPassword.isBlank()) {
                textError.setText(R.string.both_password_fields_required);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                textError.setText(R.string.passwords_do_not_match);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (newPassword.length() < 6) {
                textError.setText(R.string.new_password_min_length);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            viewModel.changePassword(oldPassword, newPassword);
            dialog.dismiss();
        });
    }

    private void showAboutDialog() {
        String message = getString(R.string.about_app_message,
                getString(R.string.university_name),
                com.example.uos_lms.BuildConfig.VERSION_NAME);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private int themeButtonId(ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return R.id.buttonThemeLight;
            case DARK:
                return R.id.buttonThemeDark;
            default:
                return R.id.buttonThemeSystem;
        }
    }

    private ThemeMode themeModeForButtonId(int buttonId) {
        if (buttonId == R.id.buttonThemeLight) return ThemeMode.LIGHT;
        if (buttonId == R.id.buttonThemeDark) return ThemeMode.DARK;
        return ThemeMode.SYSTEM;
    }
}
