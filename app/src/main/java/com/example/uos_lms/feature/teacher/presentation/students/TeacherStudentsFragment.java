package com.example.uos_lms.feature.teacher.presentation.students;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.example.uos_lms.core.ui.StatusChipHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherStudentsFragment extends Fragment {

    private TeacherStudentsViewModel viewModel;
    private boolean suppressSearchWatcher;

    public TeacherStudentsFragment() {
        super(R.layout.fragment_teacher_students);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_students, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherStudentsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.students_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        TextInputEditText editSearch = view.findViewById(R.id.editSearch);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!suppressSearchWatcher) viewModel.onSearchQueryChange(s.toString());
            }
        });

        Chip chipSubjectFilter = view.findViewById(R.id.chipSubjectFilter);
        chipSubjectFilter.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_subject),
                        viewModel.getUiState().getValue().getSubjects(),
                        subject -> subject.getCode() + " • " + subject.getTitle(),
                        getString(R.string.no_assigned_subjects_yet), viewModel::onSubjectSelected));

        View emptyState = view.findViewById(R.id.emptyState);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<TeacherStudentSummary> adapter = new SimpleListAdapter<>(R.layout.item_teacher_student_row, (itemView, summary, position) -> bindRow(itemView, summary));
        recyclerList.setAdapter(adapter);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navStudents);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navStudents) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.teacherDashboardFragment, false);
            else if (id == R.id.navAttendance) NavHostFragment.findNavController(this).navigate(R.id.teacherAttendanceReportsFragment);
            else if (id == R.id.navExamResult) NavHostFragment.findNavController(this).navigate(R.id.teacherExamResultWorkspaceFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            suppressSearchWatcher = true;
            String current = editSearch.getText() == null ? "" : editSearch.getText().toString();
            if (!current.equals(state.getSearchQuery())) editSearch.setText(state.getSearchQuery());
            suppressSearchWatcher = false;

            chipSubjectFilter.setChecked(state.getSelectedSubject() != null);
            chipSubjectFilter.setText(state.getSelectedSubject() != null
                    ? state.getSelectedSubject().getCode() + " • " + state.getSelectedSubject().getTitle()
                    : getString(R.string.all_subjects));

            List<TeacherStudentSummary> filtered = state.getFilteredStudents();
            boolean hasSubjects = !state.getSubjects().isEmpty();
            boolean hasStudents = !filtered.isEmpty();
            ((TextView) view.findViewById(R.id.textCount)).setText(hasSubjects
                    ? getString(R.string.student_count_format, filtered.size())
                    : getString(R.string.no_courses_assigned_yet_students));
            ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(
                    hasSubjects ? R.string.no_students_found : R.string.no_courses_assigned_yet_students);
            emptyState.setVisibility(hasStudents || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
            adapter.submitList(filtered);
            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void bindRow(View itemView, TeacherStudentSummary summary) {
        ((TextView) itemView.findViewById(R.id.textName)).setText(summary.getUser().getFullName());

        String roll = summary.getUser().getRollNumber();
        String reg = summary.getUser().getRegistrationNumber();
        List<String> rollReg = new ArrayList<>();
        if (!TextUtils.isEmpty(roll)) rollReg.add(getString(R.string.roll_no_format, roll));
        if (!TextUtils.isEmpty(reg)) rollReg.add(reg);
        ((TextView) itemView.findViewById(R.id.textRollReg)).setText(TextUtils.join("  •  ", rollReg));

        List<String> meta = new ArrayList<>();
        if (summary.getDepartmentName() != null) meta.add(summary.getDepartmentName());
        if (summary.getSessionLabel() != null) meta.add(summary.getSessionLabel());
        if (summary.getSemesterLabel() != null) meta.add(summary.getSemesterLabel());
        TextView textAcademicMeta = itemView.findViewById(R.id.textAcademicMeta);
        textAcademicMeta.setText(TextUtils.join("  •  ", meta));
        textAcademicMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        List<String> courseTitles = new ArrayList<>();
        for (Subject course : summary.getCourses()) courseTitles.add(course.getTitle());
        ((TextView) itemView.findViewById(R.id.textCourses)).setText(TextUtils.join(", ", courseTitles));

        AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                StatusChipHelper.bind(itemView.findViewById(R.id.textStatusChip), summary.getUser().getStatus()));
    }
}
