package com.example.uos_lms.feature.teacher.presentation.attendance;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.export.AttendanceReportExport;
import com.example.uos_lms.core.export.PdfReportGenerator;
import com.example.uos_lms.core.export.ShareFileHelper;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.AttendanceStatusChipHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherAttendanceReportsFragment extends Fragment {

    private TeacherAttendanceReportsViewModel viewModel;

    public TeacherAttendanceReportsFragment() {
        super(R.layout.fragment_teacher_attendance_reports);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_attendance_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherAttendanceReportsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.attendance_reports_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        Chip chipSubjectFilter = view.findViewById(R.id.chipSubjectFilter);
        chipSubjectFilter.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_subject),
                        viewModel.getUiState().getValue().getSubjects(),
                        (Subject subject) -> subject.getCode() + " • " + subject.getTitle(),
                        getString(R.string.no_assigned_subjects_yet), viewModel::onSubjectSelected));

        TextView textPercentage = view.findViewById(R.id.textPercentage);
        TextView textRecordCount = view.findViewById(R.id.textRecordCount);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_recorded);
        RecyclerView recyclerRecords = view.findViewById(R.id.recyclerRecords);
        FloatingActionButton fabExport = view.findViewById(R.id.fabExport);

        recyclerRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        SimpleListAdapter<com.example.uos_lms.core.domain.model.AttendanceRecord> adapter = new SimpleListAdapter<>(
                R.layout.item_attendance_record, (itemView, record, position) -> {
                    ((TextView) itemView.findViewById(R.id.textStudentName)).setText(record.getStudentName());
                    ((TextView) itemView.findViewById(R.id.textDate)).setText(DateKeyUtils.dateKeyToDisplay(record.getDateKey()));
                    int accentColorRes = AttendanceStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), record.getStatus());
                    AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);
                });
        recyclerRecords.setAdapter(adapter);

        fabExport.setOnClickListener(v -> showExportChoiceDialog(view));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navAttendance);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navAttendance) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.teacherDashboardFragment, false);
            else if (id == R.id.navStudents) NavHostFragment.findNavController(this).navigate(R.id.teacherStudentsFragment);
            else if (id == R.id.navExamResult) NavHostFragment.findNavController(this).navigate(R.id.teacherExamResultWorkspaceFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            chipSubjectFilter.setText(state.getSelectedSubject() != null
                    ? state.getSelectedSubject().getCode() + " • " + state.getSelectedSubject().getTitle()
                    : getString(R.string.all_subjects));
            chipSubjectFilter.setChecked(state.getSelectedSubject() != null);

            textPercentage.setText(getString(R.string.percent_present, state.getPercentage()));
            textRecordCount.setText(getString(R.string.records_count, state.getPresentCount(), state.getRecords().size()));

            boolean hasRecords = !state.getRecords().isEmpty();
            emptyState.setVisibility(hasRecords || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerRecords.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRecords());

            fabExport.setVisibility(hasRecords ? View.VISIBLE : View.GONE);

            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showExportChoiceDialog(View view) {
        String[] options = {getString(R.string.export_as_pdf), getString(R.string.export_as_csv)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.export_attendance)
                .setItems(options, (dialog, which) -> {
                    java.util.List<com.example.uos_lms.core.domain.model.AttendanceRecord> records = viewModel.getUiState().getValue().getRecords();
                    try {
                        if (which == 0) {
                            byte[] pdf = PdfReportGenerator.generate(getString(R.string.attendance_report_pdf_title),
                                    AttendanceReportExport.headers(), AttendanceReportExport.rows(records));
                            ShareFileHelper.sharePdf(requireContext(), "attendance_report.pdf", pdf, getString(R.string.export_attendance));
                        } else {
                            ShareFileHelper.shareCsv(requireContext(), "attendance_report.csv",
                                    AttendanceReportExport.csv(records), getString(R.string.export_attendance));
                        }
                    } catch (IllegalStateException e) {
                        Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                })
                .show();
    }
}
