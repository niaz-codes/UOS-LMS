package com.example.uos_lms.feature.hod.presentation.attendance;

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
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.AttendanceStatusChipHelper;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.chip.Chip;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodTeacherAttendanceReportsFragment extends Fragment {

    private HodTeacherAttendanceReportsViewModel viewModel;

    public HodTeacherAttendanceReportsFragment() {
        super(R.layout.fragment_hod_teacher_attendance_reports);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_teacher_attendance_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodTeacherAttendanceReportsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.teacher_attendance_reports_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        Chip chipTeacherFilter = view.findViewById(R.id.chipTeacherFilter);
        chipTeacherFilter.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_teacher),
                        viewModel.getUiState().getValue().getTeachers(), User::getFullName,
                        getString(R.string.no_teachers_in_department), viewModel::onTeacherSelected));

        Chip chipSubjectFilter = view.findViewById(R.id.chipSubjectFilter);
        chipSubjectFilter.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_course),
                        viewModel.getUiState().getValue().getSubjects(),
                        subject -> subject.getCode() + " - " + subject.getTitle(),
                        getString(R.string.no_courses_with_teacher_in_semester), viewModel::onSubjectSelected));

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

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            chipTeacherFilter.setText(state.getSelectedTeacher() != null
                    ? state.getSelectedTeacher().getFullName() : getString(R.string.all_teachers));
            chipTeacherFilter.setChecked(state.getSelectedTeacher() != null);

            chipSubjectFilter.setText(state.getSelectedSubject() != null
                    ? state.getSelectedSubject().getCode() : getString(R.string.all_courses));
            chipSubjectFilter.setChecked(state.getSelectedSubject() != null);

            textPercentage.setText(getString(R.string.percent_present, state.getPercentage()));
            textRecordCount.setText(getString(R.string.records_count, state.getPresentCount(), state.getRecords().size()));

            boolean hasRecords = !state.getRecords().isEmpty();
            emptyState.setVisibility(hasRecords || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerRecords.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRecords());
        });
    }
}
