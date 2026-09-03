package com.example.uos_lms.feature.admin.presentation.examschedule;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.common.TimeOfDayUtils;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.ExamSchedule;
import com.example.uos_lms.core.domain.model.ExamScheduleStatus;
import com.example.uos_lms.core.domain.model.ExamType;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.ExamScheduleStatusChipHelper;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminExamScheduleFragment extends Fragment {

    private AdminExamScheduleViewModel viewModel;

    public AdminExamScheduleFragment() {
        super(R.layout.fragment_admin_examschedule);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_examschedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminExamScheduleViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.exam_schedule_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_exam_schedules_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<ExamSchedule> adapter = new SimpleListAdapter<>(R.layout.item_exam_schedule_card, (itemView, schedule, position) -> {
            bindScheduleCard(itemView, schedule, true);
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.buttonSelectDepartment).setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.select_department),
                        viewModel.getUiState().getValue().getDepartments(), Department::getName,
                        getString(R.string.no_departments_exist), viewModel::selectDepartment));

        view.findViewById(R.id.buttonSelectSemester).setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.timetable_select_semester),
                        viewModel.getUiState().getValue().getSemesters(), Semester::getDisplayName,
                        getString(R.string.no_semesters_exist), viewModel::selectSemester));

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> showAddScheduleDialog());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            ((TextView) view.findViewById(R.id.rowDepartment).findViewById(R.id.textLabel)).setText(R.string.label_department);
            ((TextView) view.findViewById(R.id.rowDepartment).findViewById(R.id.textValue)).setText(
                    state.getSelectedDepartment() != null ? state.getSelectedDepartment().getName() : getString(R.string.not_selected));
            ((TextView) view.findViewById(R.id.rowSemester).findViewById(R.id.textLabel)).setText(R.string.label_semester);
            ((TextView) view.findViewById(R.id.rowSemester).findViewById(R.id.textValue)).setText(
                    state.getSelectedSemester() != null ? state.getSelectedSemester().getDisplayName() : getString(R.string.not_selected));
            view.findViewById(R.id.buttonSelectSemester).setEnabled(state.getSelectedDepartment() != null);

            boolean semesterChosen = state.getSelectedSemester() != null;
            view.findViewById(R.id.fabAdd).setVisibility(semesterChosen ? View.VISIBLE : View.GONE);

            boolean hasSchedules = !state.getSortedSchedules().isEmpty();
            emptyState.setVisibility(semesterChosen && !hasSchedules ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasSchedules ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSortedSchedules());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

    private void bindScheduleCard(View itemView, ExamSchedule schedule, boolean adminControls) {
        ((TextView) itemView.findViewById(R.id.textExamType)).setText(
                schedule.getExamType() == ExamType.MID_TERM ? R.string.exam_type_mid_term : R.string.exam_type_final_term);
        ((TextView) itemView.findViewById(R.id.textSubject)).setText(schedule.getSubjectCode() + " • " + schedule.getSubjectTitle());
        ((TextView) itemView.findViewById(R.id.textDateTime)).setText(DateKeyUtils.millisToDisplay(schedule.getExamDateMillis())
                + " • " + TimeOfDayUtils.formatRange(schedule.getStartTimeMinutes(), schedule.getEndTimeMinutes()));
        ((TextView) itemView.findViewById(R.id.textRoom)).setText(getString(R.string.timetable_room_format, schedule.getRoom()));

        TextView textInvigilator = itemView.findViewById(R.id.textInvigilator);
        if (!schedule.getInvigilatorName().isBlank()) {
            textInvigilator.setText(getString(R.string.exam_invigilator_format, schedule.getInvigilatorName()));
            textInvigilator.setVisibility(View.VISIBLE);
        } else {
            textInvigilator.setVisibility(View.GONE);
        }

        com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                ExamScheduleStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), schedule.getStatus()));

        View actionsRow = itemView.findViewById(R.id.adminActionsRow);
        actionsRow.setVisibility(adminControls ? View.VISIBLE : View.GONE);
        if (adminControls) {
            MaterialButton buttonPrimary = itemView.findViewById(R.id.buttonPrimaryAction);
            if (schedule.getStatus() == ExamScheduleStatus.DRAFT) {
                buttonPrimary.setVisibility(View.VISIBLE);
                buttonPrimary.setText(R.string.exam_publish_button);
                buttonPrimary.setOnClickListener(v -> viewModel.publish(schedule));
            } else if (schedule.getStatus() == ExamScheduleStatus.PUBLISHED) {
                buttonPrimary.setVisibility(View.VISIBLE);
                buttonPrimary.setText(R.string.exam_lock_button);
                buttonPrimary.setOnClickListener(v -> viewModel.lock(schedule));
            } else {
                buttonPrimary.setVisibility(View.GONE);
            }
            itemView.findViewById(R.id.buttonDelete).setOnClickListener(v -> viewModel.deleteSchedule(schedule));
        }
    }

    private void showAddScheduleDialog() {
        AdminExamScheduleUiState state = viewModel.getUiState().getValue();
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_exam_schedule, null);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleExamType);
        MaterialButton buttonSubject = dialogView.findViewById(R.id.buttonPickSubject);
        MaterialButton buttonDate = dialogView.findViewById(R.id.buttonPickDate);
        MaterialButton buttonStart = dialogView.findViewById(R.id.buttonPickStartTime);
        MaterialButton buttonEnd = dialogView.findViewById(R.id.buttonPickEndTime);
        MaterialButton buttonInvigilator = dialogView.findViewById(R.id.buttonPickInvigilator);
        TextInputEditText editRoom = dialogView.findViewById(R.id.editRoom);
        TextView textError = dialogView.findViewById(R.id.textError);

        toggleType.check(R.id.buttonTypeMid);
        Subject[] pickedSubject = {null};
        Long[] pickedDate = {null};
        Integer[] startMinutes = {null};
        Integer[] endMinutes = {null};
        User[] pickedInvigilator = {null};

        buttonSubject.setOnClickListener(v -> SelectDialogHelper.show(requireContext(), getString(R.string.timetable_pick_subject),
                state.getSubjects(), s -> s.getCode() + " - " + s.getTitle(), getString(R.string.no_subjects_for_semester), subject -> {
                    pickedSubject[0] = subject;
                    buttonSubject.setText(subject.getCode() + " - " + subject.getTitle());
                }));

        buttonDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (pickedDate[0] != null) calendar.setTimeInMillis(pickedDate[0]);
            new DatePickerDialog(requireContext(), (dp, year, month, dayOfMonth) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, dayOfMonth, 0, 0, 0);
                picked.set(Calendar.MILLISECOND, 0);
                pickedDate[0] = picked.getTimeInMillis();
                buttonDate.setText(DateKeyUtils.millisToDisplay(pickedDate[0]));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        buttonStart.setOnClickListener(v -> new TimePickerDialog(requireContext(), (tp, hourOfDay, minute) -> {
            startMinutes[0] = hourOfDay * 60 + minute;
            buttonStart.setText(getString(R.string.timetable_start_time_format, TimeOfDayUtils.format(startMinutes[0])));
        }, 9, 0, false).show());

        buttonEnd.setOnClickListener(v -> new TimePickerDialog(requireContext(), (tp, hourOfDay, minute) -> {
            endMinutes[0] = hourOfDay * 60 + minute;
            buttonEnd.setText(getString(R.string.timetable_end_time_format, TimeOfDayUtils.format(endMinutes[0])));
        }, 11, 0, false).show());

        buttonInvigilator.setOnClickListener(v -> SelectDialogHelper.show(requireContext(), getString(R.string.exam_pick_invigilator),
                state.getTeachers(), User::getFullName, getString(R.string.no_teachers_available), teacher -> {
                    pickedInvigilator[0] = teacher;
                    buttonInvigilator.setText(getString(R.string.exam_invigilator_format, teacher.getFullName()));
                }));

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.exam_schedule_add)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String room = editRoom.getText() == null ? "" : editRoom.getText().toString().trim();
            if (pickedSubject[0] == null) {
                textError.setText(R.string.timetable_pick_subject_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (pickedDate[0] == null) {
                textError.setText(R.string.pick_a_date_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (startMinutes[0] == null || endMinutes[0] == null) {
                textError.setText(R.string.timetable_pick_times_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (endMinutes[0] <= startMinutes[0]) {
                textError.setText(R.string.timetable_invalid_time_range_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (room.isEmpty()) {
                textError.setText(R.string.timetable_room_required_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            ExamType type = toggleType.getCheckedButtonId() == R.id.buttonTypeFinal ? ExamType.FINAL_TERM : ExamType.MID_TERM;
            viewModel.addSchedule(pickedSubject[0], type, pickedDate[0], startMinutes[0], endMinutes[0], room, pickedInvigilator[0]);
            dialog.dismiss();
        });
    }
}
