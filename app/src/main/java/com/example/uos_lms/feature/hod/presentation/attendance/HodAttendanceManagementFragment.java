package com.example.uos_lms.feature.hod.presentation.attendance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodAttendanceManagementFragment extends Fragment {

    private HodAttendanceManagementViewModel viewModel;

    public HodAttendanceManagementFragment() {
        super(R.layout.fragment_hod_attendance_management);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_attendance_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodAttendanceManagementViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View sectionStudents = view.findViewById(R.id.sectionStudents);
        View sectionTeachers = view.findViewById(R.id.sectionTeachers);
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean showStudents = checkedId == R.id.buttonToggleStudents;
            sectionStudents.setVisibility(showStudents ? View.VISIBLE : View.GONE);
            sectionTeachers.setVisibility(showStudents ? View.GONE : View.VISIBLE);
        });

        Chip chipSemester = view.findViewById(R.id.chipSemester);
        Chip chipSubject = view.findViewById(R.id.chipSubject);
        MaterialButton buttonOpenSubjectAttendance = view.findViewById(R.id.buttonOpenSubjectAttendance);
        MaterialButton buttonViewStudentReports = view.findViewById(R.id.buttonViewStudentReports);

        chipSemester.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_semester),
                        viewModel.getUiState().getValue().getSemesters(), Semester::getDisplayName,
                        getString(R.string.no_semesters_exist_in_department), viewModel::onSemesterSelected));

        chipSubject.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.select_subject),
                        viewModel.getUiState().getValue().getSubjectsInSemester(),
                        subject -> subject.getCode() + " - " + subject.getTitle(),
                        getString(R.string.no_subjects_in_semester), viewModel::onSubjectSelected));

        buttonOpenSubjectAttendance.setOnClickListener(v -> {
            Subject subject = viewModel.getUiState().getValue().getSelectedSubject();
            if (subject == null) return;
            Bundle navArgs = new Bundle();
            navArgs.putString("subjectId", subject.getId());
            navArgs.putString("departmentId", subject.getDepartmentId());
            navArgs.putString("semesterId", subject.getSemesterId());
            NavHostFragment.findNavController(this).navigate(R.id.teacherSubjectAttendanceFragment, navArgs);
        });

        buttonViewStudentReports.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.hodAttendanceReportsFragment));

        Chip chipTeacherSemester = view.findViewById(R.id.chipTeacherSemester);
        MaterialButton buttonOpenTeacherAttendance = view.findViewById(R.id.buttonOpenTeacherAttendance);
        MaterialButton buttonViewTeacherReports = view.findViewById(R.id.buttonViewTeacherReports);

        chipTeacherSemester.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_semester),
                        viewModel.getUiState().getValue().getSemesters(), Semester::getDisplayName,
                        getString(R.string.no_semesters_exist_in_department), viewModel::onTeacherSemesterSelected));

        buttonOpenTeacherAttendance.setOnClickListener(v -> {
            var state = viewModel.getUiState().getValue();
            Semester semester = state.getSelectedTeacherSemester();
            String departmentId = state.getDepartmentId();
            if (semester == null || departmentId == null) return;
            Bundle navArgs = new Bundle();
            navArgs.putString("departmentId", departmentId);
            navArgs.putString("semesterId", semester.getId());
            navArgs.putString("dateKey", com.example.uos_lms.core.common.DateKeyUtils.todayDateKey());
            NavHostFragment.findNavController(this).navigate(R.id.markTeacherAttendanceFragment, navArgs);
        });

        buttonViewTeacherReports.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.hodTeacherAttendanceReportsFragment));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            chipSemester.setText(state.getSelectedSemester() != null
                    ? state.getSelectedSemester().getDisplayName() : getString(R.string.select_semester));
            chipSemester.setChecked(state.getSelectedSemester() != null);

            chipSubject.setEnabled(state.getSelectedSemester() != null);
            chipSubject.setText(state.getSelectedSubject() != null
                    ? state.getSelectedSubject().getCode() : getString(R.string.select_subject));
            chipSubject.setChecked(state.getSelectedSubject() != null);

            buttonOpenSubjectAttendance.setEnabled(state.getSelectedSubject() != null);

            chipTeacherSemester.setText(state.getSelectedTeacherSemester() != null
                    ? state.getSelectedTeacherSemester().getDisplayName() : getString(R.string.select_semester));
            chipTeacherSemester.setChecked(state.getSelectedTeacherSemester() != null);
            buttonOpenTeacherAttendance.setEnabled(state.getSelectedTeacherSemester() != null);
        });
    }
}
