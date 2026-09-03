package com.example.uos_lms.feature.admin.presentation.timetable;

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
import com.example.uos_lms.core.common.TimeOfDayUtils;
import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.TimetableSlot;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminTimetableFragment extends Fragment {

    private AdminTimetableViewModel viewModel;

    public AdminTimetableFragment() {
        super(R.layout.fragment_admin_timetable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminTimetableViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.timetable_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.timetable_no_slots_this_day);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<TimetableSlot> adapter = new SimpleListAdapter<>(R.layout.item_timetable_slot_card, (itemView, slot, position) -> {
            ((TextView) itemView.findViewById(R.id.textTime)).setText(TimeOfDayUtils.formatRange(slot.getStartTimeMinutes(), slot.getEndTimeMinutes()));
            ((TextView) itemView.findViewById(R.id.textSubject)).setText(slot.getSubjectCode() + " • " + slot.getSubjectTitle());
            ((TextView) itemView.findViewById(R.id.textTeacher)).setText(slot.getTeacherName().isBlank()
                    ? getString(R.string.teacher_not_assigned) : getString(R.string.teacher_label_format, slot.getTeacherName()));
            ((TextView) itemView.findViewById(R.id.textRoom)).setText(getString(R.string.timetable_room_format, slot.getRoom()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_admin_start);
            View buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonDelete.setVisibility(View.VISIBLE);
            buttonDelete.setOnClickListener(v -> viewModel.deleteSlot(slot));
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

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutDays);
        for (DayOfWeek day : DayOfWeek.values()) {
            tabLayout.addTab(tabLayout.newTab().setText(day.getDisplayName().substring(0, 3)));
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.selectDay(DayOfWeek.values()[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> showAddSlotDialog());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            ((TextView) view.findViewById(R.id.rowDepartment).findViewById(R.id.textLabel)).setText(R.string.label_department);
            ((TextView) view.findViewById(R.id.rowDepartment).findViewById(R.id.textValue)).setText(
                    state.getSelectedDepartment() != null ? state.getSelectedDepartment().getName() : getString(R.string.not_selected));
            ((TextView) view.findViewById(R.id.rowSemester).findViewById(R.id.textLabel)).setText(R.string.label_semester);
            ((TextView) view.findViewById(R.id.rowSemester).findViewById(R.id.textValue)).setText(
                    state.getSelectedSemester() != null ? state.getSelectedSemester().getDisplayName() : getString(R.string.not_selected));
            view.findViewById(R.id.buttonSelectSemester).setEnabled(state.getSelectedDepartment() != null);

            boolean semesterChosen = state.getSelectedSemester() != null;
            tabLayout.setVisibility(semesterChosen ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.fabAdd).setVisibility(semesterChosen ? View.VISIBLE : View.GONE);

            boolean hasSlots = !state.getSlotsForSelectedDay().isEmpty();
            emptyState.setVisibility(semesterChosen && !hasSlots ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(semesterChosen && hasSlots ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSlotsForSelectedDay());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

    private void showAddSlotDialog() {
        AdminTimetableUiState state = viewModel.getUiState().getValue();
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_timetable_slot, null);
        MaterialButton buttonSubject = dialogView.findViewById(R.id.buttonPickSubject);
        MaterialButton buttonDay = dialogView.findViewById(R.id.buttonPickDay);
        MaterialButton buttonStart = dialogView.findViewById(R.id.buttonPickStartTime);
        MaterialButton buttonEnd = dialogView.findViewById(R.id.buttonPickEndTime);
        TextInputEditText editRoom = dialogView.findViewById(R.id.editRoom);
        TextView textError = dialogView.findViewById(R.id.textError);

        Subject[] pickedSubject = {null};
        DayOfWeek[] pickedDay = {state.getSelectedDay()};
        Integer[] startMinutes = {null};
        Integer[] endMinutes = {null};

        buttonDay.setText(getString(R.string.timetable_pick_day_format, pickedDay[0].getDisplayName()));

        buttonSubject.setOnClickListener(v -> SelectDialogHelper.show(requireContext(), getString(R.string.timetable_pick_subject),
                state.getSubjects(), s -> s.getCode() + " - " + s.getTitle(), getString(R.string.no_subjects_for_semester), subject -> {
                    pickedSubject[0] = subject;
                    buttonSubject.setText(subject.getCode() + " - " + subject.getTitle());
                }));

        buttonDay.setOnClickListener(v -> SelectDialogHelper.show(requireContext(), getString(R.string.timetable_pick_day),
                Arrays.asList(DayOfWeek.values()), DayOfWeek::getDisplayName, "", day -> {
                    pickedDay[0] = day;
                    buttonDay.setText(getString(R.string.timetable_pick_day_format, day.getDisplayName()));
                }));

        buttonStart.setOnClickListener(v -> pickTime(minutes -> {
            startMinutes[0] = minutes;
            buttonStart.setText(getString(R.string.timetable_start_time_format, TimeOfDayUtils.format(minutes)));
        }));
        buttonEnd.setOnClickListener(v -> pickTime(minutes -> {
            endMinutes[0] = minutes;
            buttonEnd.setText(getString(R.string.timetable_end_time_format, TimeOfDayUtils.format(minutes)));
        }));

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.timetable_add_slot)
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
            viewModel.addSlot(pickedSubject[0], pickedDay[0], startMinutes[0], endMinutes[0], room);
            dialog.dismiss();
        });
    }

    private interface TimePicked {
        void onPicked(int minutesSinceMidnight);
    }

    private void pickTime(TimePicked callback) {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) ->
                callback.onPicked(hourOfDay * 60 + minute), 8, 0, false).show();
    }
}
