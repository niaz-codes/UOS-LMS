package com.example.uos_lms.feature.admin.presentation.attendance;

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
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.AttendanceStatusChipHelper;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminTeacherAttendanceFragment extends Fragment {

    private AdminTeacherAttendanceViewModel viewModel;

    public AdminTeacherAttendanceFragment() {
        super(R.layout.fragment_admin_teacher_attendance);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_teacher_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminTeacherAttendanceViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.teacher_attendance_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        Chip chipDepartment = view.findViewById(R.id.chipDepartment);
        Chip chipTeacher = view.findViewById(R.id.chipTeacher);
        MaterialButton buttonMarkAttendance = view.findViewById(R.id.buttonMarkAttendance);
        TextView textPercentage = view.findViewById(R.id.textPercentage);
        TextView textRecordCount = view.findViewById(R.id.textRecordCount);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_recorded);
        RecyclerView recyclerRecords = view.findViewById(R.id.recyclerRecords);

        recyclerRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        SimpleListAdapter<com.example.uos_lms.core.domain.model.TeacherAttendanceRecord> adapter = new SimpleListAdapter<>(
                R.layout.item_attendance_record, (itemView, record, position) -> {
                    ((TextView) itemView.findViewById(R.id.textStudentName)).setText(record.getTeacherName());
                    ((TextView) itemView.findViewById(R.id.textDate)).setText(getString(R.string.course_and_date_format,
                            record.getSubjectCode(), record.getSubjectTitle(), DateKeyUtils.dateKeyToDisplay(record.getDateKey())));
                    int accentColorRes = AttendanceStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), record.getStatus());
                    AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);
                });
        recyclerRecords.setAdapter(adapter);

        chipDepartment.setOnClickListener(v -> {
            var state = viewModel.getUiState().getValue();
            if (state == null) return;
            SelectDialogHelper.show(requireContext(), getString(R.string.choose_department), state.getDepartments(),
                    Department::getName, getString(R.string.no_departments_yet), viewModel::onDepartmentSelected);
        });
        chipTeacher.setOnClickListener(v -> {
            var state = viewModel.getUiState().getValue();
            if (state == null || state.getSelectedDepartment() == null) return;
            SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_teacher), state.getTeachers(),
                    User::getFullName, getString(R.string.no_teachers_in_department), viewModel::onTeacherSelected);
        });

        Chip chipMarkSemester = view.findViewById(R.id.chipMarkSemester);
        chipMarkSemester.setOnClickListener(v -> {
            var state = viewModel.getUiState().getValue();
            if (state == null || state.getSelectedDepartment() == null) return;
            SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_semester), state.getSemesters(),
                    Semester::getDisplayName, getString(R.string.no_semesters_exist_in_department), viewModel::onSemesterSelected);
        });

        buttonMarkAttendance.setOnClickListener(v -> {
            var state = viewModel.getUiState().getValue();
            Department department = state.getSelectedDepartment();
            Semester semester = state.getSelectedSemester();
            if (department == null || semester == null) return;
            Bundle navArgs = new Bundle();
            navArgs.putString("departmentId", department.getId());
            navArgs.putString("semesterId", semester.getId());
            navArgs.putString("dateKey", DateKeyUtils.todayDateKey());
            NavHostFragment.findNavController(this).navigate(R.id.markTeacherAttendanceFragment, navArgs);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            chipDepartment.setText(state.getSelectedDepartment() != null
                    ? state.getSelectedDepartment().getName() : getString(R.string.all_departments));
            chipDepartment.setChecked(state.getSelectedDepartment() != null);

            chipTeacher.setEnabled(state.getSelectedDepartment() != null);
            chipTeacher.setText(state.getSelectedTeacher() != null
                    ? state.getSelectedTeacher().getFullName() : getString(R.string.all_teachers));
            chipTeacher.setChecked(state.getSelectedTeacher() != null);

            chipMarkSemester.setEnabled(state.getSelectedDepartment() != null);
            chipMarkSemester.setText(state.getSelectedSemester() != null
                    ? state.getSelectedSemester().getDisplayName() : getString(R.string.select_semester));
            chipMarkSemester.setChecked(state.getSelectedSemester() != null);

            buttonMarkAttendance.setEnabled(state.getSelectedDepartment() != null && state.getSelectedSemester() != null);

            textPercentage.setText(getString(R.string.percent_present, state.getPercentage()));
            textRecordCount.setText(getString(R.string.records_count, state.getPresentCount(), state.getRecords().size()));

            boolean hasRecords = !state.getRecords().isEmpty();
            emptyState.setVisibility(hasRecords || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerRecords.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRecords());
        });
    }
}
