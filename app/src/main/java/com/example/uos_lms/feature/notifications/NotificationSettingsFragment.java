package com.example.uos_lms.feature.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotificationSettingsFragment extends Fragment {

    private NotificationSettingsViewModel viewModel;

    public NotificationSettingsFragment() {
        super(R.layout.fragment_notification_settings);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationSettingsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.action_notification_settings_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        bindRow(view, R.id.rowPushEnabled, R.drawable.ic_notifications, R.string.notification_push_title, R.string.notification_push_subtitle,
                viewModel::setPushEnabled);
        bindRow(view, R.id.rowSoundEnabled, R.drawable.ic_volume_up, R.string.notification_sound_title, R.string.notification_sound_subtitle,
                viewModel::setSoundEnabled);
        bindRow(view, R.id.rowVibrationEnabled, R.drawable.ic_vibration, R.string.notification_vibration_title, R.string.notification_vibration_subtitle,
                viewModel::setVibrationEnabled);

        bindCategoryRow(view, R.id.rowAccount, R.drawable.ic_person, NotificationCategory.ACCOUNT, R.string.category_account, R.string.category_account_subtitle);
        bindCategoryRow(view, R.id.rowAcademic, R.drawable.ic_folder_open, NotificationCategory.ACADEMIC_CONTENT, R.string.category_academic_content, R.string.category_academic_content_subtitle);
        bindCategoryRow(view, R.id.rowAttendance, R.drawable.ic_event_busy, NotificationCategory.ATTENDANCE, R.string.category_attendance, R.string.category_attendance_subtitle);
        bindCategoryRow(view, R.id.rowExamResult, R.drawable.ic_grade, NotificationCategory.EXAM_RESULT, R.string.category_exam_result, R.string.category_exam_result_subtitle);
        bindCategoryRow(view, R.id.rowLeave, R.drawable.ic_event_busy, NotificationCategory.LEAVE, R.string.category_leave, R.string.category_leave_subtitle);
        bindCategoryRow(view, R.id.rowAnnouncement, R.drawable.ic_campaign, NotificationCategory.ANNOUNCEMENT, R.string.category_announcement, R.string.category_announcement_subtitle);
        bindCategoryRow(view, R.id.rowCalendar, R.drawable.ic_calendar_month, NotificationCategory.CALENDAR, R.string.category_calendar, R.string.category_calendar_subtitle);
        bindCategoryRow(view, R.id.rowMessage, R.drawable.ic_chat, NotificationCategory.MESSAGE, R.string.category_message, R.string.category_message_subtitle);
        bindCategoryRow(view, R.id.rowPromotion, R.drawable.ic_school, NotificationCategory.PROMOTION, R.string.category_promotion, R.string.category_promotion_subtitle);
        bindCategoryRow(view, R.id.rowSystem, R.drawable.ic_campaign, NotificationCategory.SYSTEM, R.string.category_system, R.string.category_system_subtitle);

        view.findViewById(R.id.buttonSystemSettings).setOnClickListener(v -> openSystemNotificationSettings());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void openSystemNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        startActivity(intent);
    }

    private interface ToggleSetter {
        void set(boolean checked);
    }

    private void bindRow(View root, int rowId, int iconRes, int titleRes, int subtitleRes, ToggleSetter setter) {
        View row = root.findViewById(rowId);
        ((ImageView) row.findViewById(R.id.imageIcon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.textTitle)).setText(titleRes);
        ((TextView) row.findViewById(R.id.textSubtitle)).setText(subtitleRes);
        MaterialSwitch materialSwitch = row.findViewById(R.id.switchToggle);
        materialSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (buttonView.isPressed()) setter.set(checked);
        });
    }

    private void bindCategoryRow(View root, int rowId, int iconRes, NotificationCategory category, int titleRes, int subtitleRes) {
        bindRow(root, rowId, iconRes, titleRes, subtitleRes, checked -> viewModel.setCategoryEnabled(category, checked));
    }

    private void render(View root, NotificationSettingsUiState state) {
        View progressLoading = root.findViewById(R.id.progressLoading);
        View contentContainer = root.findViewById(R.id.contentContainer);
        progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(state.isLoading() ? View.GONE : View.VISIBLE);
        if (state.isLoading()) return;

        setSwitch(root, R.id.rowPushEnabled, state.getSettings().isPushEnabled());
        setSwitch(root, R.id.rowSoundEnabled, state.getSettings().isSoundEnabled());
        setSwitch(root, R.id.rowVibrationEnabled, state.getSettings().isVibrationEnabled());
        setSwitch(root, R.id.rowAccount, state.getSettings().isAccount());
        setSwitch(root, R.id.rowAcademic, state.getSettings().isAcademicContent());
        setSwitch(root, R.id.rowAttendance, state.getSettings().isAttendance());
        setSwitch(root, R.id.rowExamResult, state.getSettings().isExamResult());
        setSwitch(root, R.id.rowLeave, state.getSettings().isLeave());
        setSwitch(root, R.id.rowAnnouncement, state.getSettings().isAnnouncement());
        setSwitch(root, R.id.rowCalendar, state.getSettings().isCalendar());
        setSwitch(root, R.id.rowMessage, state.getSettings().isMessage());
        setSwitch(root, R.id.rowPromotion, state.getSettings().isPromotion());
        setSwitch(root, R.id.rowSystem, state.getSettings().isSystem());

        if (state.getErrorMessage() != null) {
            Snackbar.make(root, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.clearError();
        }
    }

    private void setSwitch(View root, int rowId, boolean checked) {
        MaterialSwitch materialSwitch = root.findViewById(rowId).findViewById(R.id.switchToggle);
        if (materialSwitch.isChecked() != checked) {
            materialSwitch.setChecked(checked);
        }
    }
}
